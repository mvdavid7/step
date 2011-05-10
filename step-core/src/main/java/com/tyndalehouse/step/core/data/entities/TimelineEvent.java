package com.tyndalehouse.step.core.data.entities;

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import org.joda.time.LocalDateTime;

import com.avaje.ebean.annotation.CacheStrategy;
import com.tyndalehouse.step.core.data.common.PrecisionType;

/**
 * Represents an event or duration in time.
 * 
 * @author Chris
 */
@CacheStrategy(readOnly = true)
@Entity
@DiscriminatorValue("1")
public class TimelineEvent extends ScriptureTarget implements Serializable {
    private static final long serialVersionUID = -4642904574412249515L;

    @Column
    private String summary;

    @Column(nullable = true)
    private LocalDateTime fromDate;

    @Column(nullable = true)
    private LocalDateTime toDate;

    @Column(nullable = true)
    private PrecisionType fromPrecision;

    @Column(nullable = true)
    private PrecisionType toPrecision;

    @ManyToOne(cascade = CascadeType.ALL)
    private HotSpot hotSpot;

    /**
     * @return the summary
     */
    public String getSummary() {
        return this.summary;
    }

    /**
     * @param summary the summary to set
     */
    public void setSummary(final String summary) {
        this.summary = summary;
    }

    /**
     * @return the fromDate
     */
    public LocalDateTime getFromDate() {
        return this.fromDate;
    }

    /**
     * @param fromDate the fromDate to set
     */
    public void setFromDate(final LocalDateTime fromDate) {
        this.fromDate = fromDate;
    }

    /**
     * @return the toDate
     */
    public LocalDateTime getToDate() {
        return this.toDate;
    }

    /**
     * @param toDate the toDate to set
     */
    public void setToDate(final LocalDateTime toDate) {
        this.toDate = toDate;
    }

    /**
     * @return the fromPrecision
     */
    public PrecisionType getFromPrecision() {
        return this.fromPrecision;
    }

    /**
     * @param fromPrecision the fromPrecision to set
     */
    public void setFromPrecision(final PrecisionType fromPrecision) {
        this.fromPrecision = fromPrecision;
    }

    /**
     * @return the toPrecision
     */
    public PrecisionType getToPrecision() {
        return this.toPrecision;
    }

    /**
     * @param toPrecision the toPrecision to set
     */
    public void setToPrecision(final PrecisionType toPrecision) {
        this.toPrecision = toPrecision;
    }

    /**
     * @return the hotSpot
     */
    public HotSpot getHotSpot() {
        return this.hotSpot;
    }

    /**
     * @param hotSpot the hotSpot to set
     */
    public void setHotSpot(final HotSpot hotSpot) {
        this.hotSpot = hotSpot;
    }

    /**
     * to get rid of a findbugs bug, we override to make clear we are using the parent's equal method
     * 
     * @param obj the object that we are comparing
     * @return true if objects are equals
     */
    @SuppressWarnings("PMD.UselessOverridingMethod")
    @Override
    public boolean equals(final Object obj) {
        return super.equals(obj);
    }

    /**
     * overriding the hashcode because we've override the equals
     * 
     * @return the parent's hashcode
     */
    @SuppressWarnings("PMD.UselessOverridingMethod")
    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
