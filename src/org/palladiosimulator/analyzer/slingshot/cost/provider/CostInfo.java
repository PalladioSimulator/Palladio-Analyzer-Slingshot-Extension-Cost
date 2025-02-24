package org.palladiosimulator.analyzer.slingshot.cost.provider;

public class CostInfo {
	private final double amount;
	private final double interval;
	
	
	public CostInfo(final double amount, final double interval) {
		super();
		this.amount = amount;
		this.interval = interval;
	}

	public double getAmount() {
		return amount;
	}

	public double getInterval() {
		return interval;
	}
}
