package org.palladiosimulator.analyzer.slingshot.cost.provider;

import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Provides the  that represents the next simulation run.
 *
 * @author Sarah Stieß
 *
 */
@Singleton
public class HackyCostProvider implements Provider<CostInfo> {

	private CostInfo info;

	public void set(final CostInfo info) {
		this.info = info;
	}

	@Override
	public CostInfo get() {
		return info;
	}
}
