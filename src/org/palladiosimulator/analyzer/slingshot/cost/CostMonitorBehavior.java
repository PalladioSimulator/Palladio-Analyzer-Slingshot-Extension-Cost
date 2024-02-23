package org.palladiosimulator.analyzer.slingshot.cost;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.common.annotations.Nullable;
import org.palladiosimulator.analyzer.slingshot.common.events.AbstractSimulationEvent;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.cost.events.IntervalPassed;
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
import org.palladiosimulator.pcm.resourceenvironment.ResourceContainer;
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
		IntervalPassed.class }, cardinality = EventCardinality.MANY)
@OnEvent(when = IntervalPassed.class, then = { ProbeTaken.class,
		IntervalPassed.class }, cardinality = EventCardinality.MANY)
public class CostMonitorBehavior implements SimulationBehaviorExtension {

	private final static Logger LOGGER = Logger.getLogger(CostMonitorBehavior.class);

	private final IGenericCalculatorFactory calculatorFactory;

	private final Map<ResourceContainer, ContainerCostProbe> probes;
	private final Configuration semanticConfiguration;

	@Inject
	public CostMonitorBehavior(final IGenericCalculatorFactory calculatorFactory,
			final @Nullable Configuration semanticConfiguration) {
		this.calculatorFactory = calculatorFactory;
		this.semanticConfiguration = semanticConfiguration;
		this.probes = new HashMap<>();
	}

	@Subscribe
	public Result<AbstractSimulationEvent> onMeasurementSpecification(final MeasurementSpecificationVisited m) {
		final MeasurementSpecification spec = m.getEntity();
		final MeasuringPoint measuringPoint = spec.getMonitor().getMeasuringPoint();

		if (measuringPoint instanceof ResourceContainerMeasuringPoint rcmp
				&& MetricDescriptionUtility.metricDescriptionIdsEqual(spec.getMetricDescription(),
					MetricDescriptionConstants.COST_OF_RESOURCE_CONTAINERS)) {
				
				
				Optional<ElasticInfrastructureCfg>  eiCfg = semanticConfiguration.getTargetCfgs().stream()
						.filter(cfg -> cfg instanceof ElasticInfrastructureCfg c)
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

				final ContainerCostProbe probe = this.probes.get(eiCfg.get().getUnit());

				return Result.of(new CalculatorRegistered(calculator),
						new IntervalPassed(rcmp.getResourceContainer(), probe.getInterval()));

		}
		return Result.empty();
	}

	@Subscribe
	public Result<AbstractSimulationEvent> onIntevalPassed(final IntervalPassed intervalPassed) {
		final ContainerCostProbe probe = this.probes.get(intervalPassed.getTargetResourceContainer());

		probe.takeMeasurement(intervalPassed);

		ProbeTaken probeTaken = new ProbeTaken(ProbeTakenEntity.builder().withProbe(probe).build());
		IntervalPassed nextIntervalPassed = new IntervalPassed(intervalPassed);

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

		ContainerCostProbe newProbe = new ContainerCostProbe(eiCfg);

		this.probes.putIfAbsent(eiCfg.getUnit(), newProbe);

		return calculatorFactory.buildCalculator(MetricDescriptionConstants.COST_OVER_TIME, measuringPoint,
				DefaultCalculatorProbeSets.createSingularProbeConfiguration(newProbe));
	}
}
