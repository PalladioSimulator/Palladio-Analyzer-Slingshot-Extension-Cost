package org.palladiosimulator.analyzer.slingshot.cost;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.common.annotations.Nullable;
import org.palladiosimulator.analyzer.slingshot.common.events.AbstractSimulationEvent;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.cost.events.TakeCostMeasurement;
import org.palladiosimulator.analyzer.slingshot.cost.probes.ContainerCostProbe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.EventCardinality;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.Result;
import org.palladiosimulator.analyzer.slingshot.monitor.data.entities.ProbeTakenEntity;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.CalculatorRegistered;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.ProbeTaken;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.modelvisited.MeasurementSpecificationVisited;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.modelvisited.MonitorModelVisited;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPoint;
import org.palladiosimulator.edp2.util.MetricDescriptionUtility;
import org.palladiosimulator.mdsdprofiles.api.StereotypeAPI;
import org.palladiosimulator.metricspec.constants.MetricDescriptionConstants;
import org.palladiosimulator.monitorrepository.MeasurementSpecification;
import org.palladiosimulator.pcmmeasuringpoint.ResourceContainerMeasuringPoint;
import org.palladiosimulator.probeframework.calculator.Calculator;
import org.palladiosimulator.probeframework.calculator.DefaultCalculatorProbeSets;
import org.palladiosimulator.probeframework.calculator.IGenericCalculatorFactory;
import org.palladiosimulator.semanticspd.Configuration;
import org.palladiosimulator.semanticspd.ElasticInfrastructureCfg;

/**
 *
 *
 *
 * @author Sarah Stieß
 *
 */
@OnEvent(when = MonitorModelVisited.class, then = { CalculatorRegistered.class,
		TakeCostMeasurement.class }, cardinality = EventCardinality.MANY)
@OnEvent(when = TakeCostMeasurement.class, then = { ProbeTaken.class,
		TakeCostMeasurement.class }, cardinality = EventCardinality.MANY)
public class CostMonitorBehavior implements SimulationBehaviorExtension {

	private final static Logger LOGGER = Logger.getLogger(CostMonitorBehavior.class);

	private final IGenericCalculatorFactory calculatorFactory;

	private final Map<String, ContainerCostProbe> probes;
	private final Configuration semanticConfiguration;

	@Inject
	public CostMonitorBehavior(final IGenericCalculatorFactory calculatorFactory,
			final @Nullable Configuration semanticConfiguration) {
		this.calculatorFactory = calculatorFactory;
		this.semanticConfiguration = semanticConfiguration;
		this.probes = new HashMap<>();
	}

	@Override
	public boolean isActive() {
		return semanticConfiguration != null;
	}

	@Subscribe
	public Result<AbstractSimulationEvent> onMeasurementSpecification(final MeasurementSpecificationVisited m) {
		final MeasurementSpecification spec = m.getEntity();
		final MeasuringPoint measuringPoint = spec.getMonitor().getMeasuringPoint();

		if (measuringPoint instanceof final ResourceContainerMeasuringPoint rcmp
				&& MetricDescriptionUtility.metricDescriptionIdsEqual(spec.getMetricDescription(),
						MetricDescriptionConstants.COST_OF_RESOURCE_CONTAINERS)) {


			final Optional<ElasticInfrastructureCfg>  eiCfg = semanticConfiguration.getTargetCfgs().stream()
					.filter(cfg -> cfg instanceof final ElasticInfrastructureCfg c)
					.map(cfg -> (ElasticInfrastructureCfg) cfg)
					.filter(cfg -> cfg.getUnit().equals(rcmp.getResourceContainer()))
					.findFirst();

			if (eiCfg.isEmpty()) {
				LOGGER.debug(String.format(
						"Not registering Calculator for %s, no ElasticInfrasturctureConfiguration with matching Unit.",
						rcmp.getStringRepresentation()));
				return Result.empty();
			}

			if (StereotypeAPI.getAppliedStereotypes(eiCfg.get().getUnit()).isEmpty()) {
				LOGGER.debug(String.format(
						"Not registering Calculator for %s, because there are no Costs defined for the Resoruce Container.",
						rcmp.getStringRepresentation()));
				return Result.empty();
			}

			final Calculator calculator = setupCalculator(rcmp, calculatorFactory, eiCfg.get());

			final ContainerCostProbe probe = this.probes.get(eiCfg.get().getUnit().getId());

			return Result.of(new CalculatorRegistered(calculator),
					new TakeCostMeasurement(0.0, rcmp.getResourceContainer(), probe.getInterval()));

		}
		return Result.empty();
	}

	@Subscribe
	public Result<AbstractSimulationEvent> onIntevalPassed(final TakeCostMeasurement intervalPassed) {
		final ContainerCostProbe probe = this.probes.get(intervalPassed.getTargetResourceContainer().getId());

		probe.takeMeasurement(intervalPassed);

		final ProbeTaken probeTaken = new ProbeTaken(ProbeTakenEntity.builder().withProbe(probe).build());
		final TakeCostMeasurement nextIntervalPassed = new TakeCostMeasurement(intervalPassed);

		return Result.of(probeTaken, nextIntervalPassed);
	}

	/**
	 *
	 * @param measuringPoint
	 * @param calculatorFactory
	 * @param eiCfg
	 * @return
	 */
	public Calculator setupCalculator(final MeasuringPoint measuringPoint,
			final IGenericCalculatorFactory calculatorFactory, final ElasticInfrastructureCfg eiCfg) {

		final ContainerCostProbe newProbe = new ContainerCostProbe(eiCfg);

		this.probes.putIfAbsent(eiCfg.getUnit().getId(), newProbe);

		return calculatorFactory.buildCalculator(MetricDescriptionConstants.COST_OVER_TIME, measuringPoint,
				DefaultCalculatorProbeSets.createSingularProbeConfiguration(newProbe));
	}
}
