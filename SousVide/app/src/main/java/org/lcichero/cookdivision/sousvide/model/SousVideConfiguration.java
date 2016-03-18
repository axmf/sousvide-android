package org.lcichero.cookdivision.sousvide.model;

public class SousVideConfiguration {

    private Float setPoint;
    private Float currentTemperature;

    public SousVideConfiguration() {
    }

    public SousVideConfiguration(Float setPoint, Float currentTemperature) {
        this.setPoint = setPoint;
        this.currentTemperature = currentTemperature;
    }

    public Float getSetPoint() {
        return setPoint;
    }

    public void setSetPoint(Float setPoint) {
        this.setPoint = setPoint;
    }

    public Float getCurrentTemperature() {
        return currentTemperature;
    }

    public void setCurrentTemperature(Float currentTemperature) {
        this.currentTemperature = currentTemperature;
    }
}