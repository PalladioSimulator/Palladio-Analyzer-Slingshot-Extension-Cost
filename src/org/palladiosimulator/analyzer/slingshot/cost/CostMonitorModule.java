package org.palladiosimulator.analyzer.slingshot.cost;


import org.palladiosimulator.analyzer.slingshot.core.extension.AbstractSlingshotExtension;

public class CostMonitorModule extends AbstractSlingshotExtension {

	@Override
	protected void configure() {
		install(CostMonitorBehavior.class);
	}

}
