package org.palladiosimulator.analyzer.slingshot.cost.probes;

import javax.measure.Measure;

import org.jscience.economics.money.Currency;
import org.jscience.economics.money.Money;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.cost.events.IntervalPassed;
import org.palladiosimulator.analyzer.slingshot.monitor.probes.EventBasedListProbe;
import org.palladiosimulator.mdsdprofiles.api.StereotypeAPI;
import org.palladiosimulator.metricspec.constants.MetricDescriptionConstants;
import org.palladiosimulator.semanticspd.ElasticInfrastructureCfg;

/**
 *
 * @author Sarah Stieß
 *
 */
public final class ContainerCostProbe extends EventBasedListProbe<Double, Money> {

	private final ElasticInfrastructureCfg elasticInfrastructureConfiguration;
	
	final double cost;
	final double interval;

	static final String STEREOTYPE = "Price";
	static final String TAG_INTERVAL = "interval";
	static final String TAG_COST = "amount";

	/**
	 * 
	 * Constructs a .
	 */
	public ContainerCostProbe(final ElasticInfrastructureCfg cfg) {
		super(MetricDescriptionConstants.COST_OVER_TIME);
		this.elasticInfrastructureConfiguration = cfg;
		

		if (StereotypeAPI.getAppliedStereotypes(this.elasticInfrastructureConfiguration.getUnit()).isEmpty()) {
			throw new IllegalArgumentException(
					String.format("Expected Stereotypes on ResourceContainer %s, but found none.",
							this.elasticInfrastructureConfiguration.getUnit().getEntityName()));
		}
		
		this.interval = StereotypeAPI.getTaggedValue(this.elasticInfrastructureConfiguration.getUnit(), TAG_INTERVAL,
				STEREOTYPE);
		this.cost = StereotypeAPI.getTaggedValue(this.elasticInfrastructureConfiguration.getUnit(), TAG_INTERVAL,
				STEREOTYPE);
		
	}

	@Override
	public Measure<Double, Money> getMeasurement(final DESEvent event) {
		if (event instanceof IntervalPassed intervalPassed) {

			assert elasticInfrastructureConfiguration.getElements()
					.contains(intervalPassed.getTargetResourceContainer());
			
			int replicas = elasticInfrastructureConfiguration.getElements().size();
			
			return Measure.valueOf(replicas * cost, Currency.EUR);
		}
		throw new IllegalArgumentException(String.format("Wrong eventype. Expected %s but got %s.",
				IntervalPassed.class.getSimpleName(), event.getClass().getSimpleName()));
	}

	/**
	 * 
	 * @return interval between two cost measurements
	 */
	public double getInterval() {
		return this.interval;
	}

}
