package org.palladiosimulator.analyzer.slingshot.cost.probes;

import javax.measure.Measure;

import org.jscience.economics.money.Currency;
import org.jscience.economics.money.Money;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.cost.events.TakeCostMeasurement;
import org.palladiosimulator.analyzer.slingshot.cost.provider.CostInfo;
import org.palladiosimulator.analyzer.slingshot.monitor.probes.EventBasedListProbe;
import org.palladiosimulator.metricspec.constants.MetricDescriptionConstants;
import org.palladiosimulator.semanticspd.ElasticInfrastructureCfg;

/**
 *
 * The Metric description we use is COST_OVER_TIME, but actually, this one
 * disregards time. this is just cost at a point in time.
 *
 * @author Sarah Stieß
 *
 */
public final class ContainerCostProbe extends EventBasedListProbe<Double, Money> {

	private final ElasticInfrastructureCfg elasticInfrastructureConfiguration;

	final double cost;
	final double interval;

//	static final String STEREOTYPE_COST = "Price";
//	static final String STEREOTYPE_COSTREPORT = "CostReport";
//	static final String TAG_INTERVAL = "interval";
//	static final String TAG_COST = "amount";

	/**
	 *
	 * Constructs a .
	 */
	public ContainerCostProbe(final ElasticInfrastructureCfg cfg, final CostInfo costInfo) {
		super(MetricDescriptionConstants.COST_OVER_TIME);
		this.elasticInfrastructureConfiguration = cfg;


//		if (StereotypeAPI.getAppliedStereotypes(this.elasticInfrastructureConfiguration.getUnit()).isEmpty()) {
//			throw new IllegalArgumentException(
//					String.format("Expected Stereotypes on ResourceContainer %s, but found none.",
//							this.elasticInfrastructureConfiguration.getUnit().getEntityName()));
//		}
//
//		this.interval = StereotypeAPI.getTaggedValue(this.elasticInfrastructureConfiguration.getResourceEnvironment(),
//				TAG_INTERVAL, STEREOTYPE_COSTREPORT);
//		this.cost = StereotypeAPI.getTaggedValue(this.elasticInfrastructureConfiguration.getUnit(), TAG_COST,
//				STEREOTYPE_COST);
		
		this.interval = costInfo.getInterval();
		this.cost = costInfo.getAmount();

	}

	@Override
	public Measure<Double, Money> getMeasurement(final DESEvent event) {
		if (event instanceof final TakeCostMeasurement intervalPassed) {

			assert elasticInfrastructureConfiguration.getElements()
			.contains(intervalPassed.getTargetResourceContainer())
			: String.format("Resourcecontainer %s[%s] is not in given configuration %s",
					intervalPassed.getTargetResourceContainer().getEntityName(),
					intervalPassed.getTargetResourceContainer().getId(),
					elasticInfrastructureConfiguration.toString());

			final int replicas = elasticInfrastructureConfiguration.getElements().size();

			return Measure.valueOf(replicas * cost, Currency.EUR);
		}
		throw new IllegalArgumentException(String.format("Wrong eventype. Expected %s but got %s.",
				TakeCostMeasurement.class.getSimpleName(), event.getClass().getSimpleName()));
	}

	/**
	 *
	 * @return interval between two cost measurements
	 */
	public double getInterval() {
		return this.interval;
	}

}
