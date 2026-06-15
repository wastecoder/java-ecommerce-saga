package com.wastecoder.shopflow.payment.adapter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Controllable settings for the PSP mock. {@code mode} selects the global decision strategy; in
 * {@link Mode#DECLINE_ABOVE_THRESHOLD} the command amount steers the outcome (an amount {@code >= threshold}
 * is declined), so a single running instance can produce both approvals and declines for tests.
 *
 * <p>Defaults are set here (not only in {@code application.yaml}) so the behaviour is defined even where the
 * yaml is shadowed — the test {@code application.yaml} shadows the main one, and these defaults keep the
 * PSP deterministic there without re-declaring the block.
 */
@ConfigurationProperties(prefix = "shopflow.payment.psp")
public class PspProperties {

	/** Decision strategy applied by the PSP mock when authorizing a payment. */
	public enum Mode {
		APPROVE_ALL,
		DECLINE_ALL,
		DECLINE_ABOVE_THRESHOLD
	}

	private Mode mode = Mode.DECLINE_ABOVE_THRESHOLD;

	private BigDecimal declineThreshold = new BigDecimal("1000.00");

	public Mode getMode() {
		return mode;
	}

	public void setMode(Mode mode) {
		this.mode = mode;
	}

	public BigDecimal getDeclineThreshold() {
		return declineThreshold;
	}

	public void setDeclineThreshold(BigDecimal declineThreshold) {
		this.declineThreshold = declineThreshold;
	}
}
