package org.acme.data;

public class Submitter {

    public Submitter() {
    }

    public Submitter(String device_id) {
        this.device_id = device_id;
    }

    public String device_id;

    public String getDevice_id() {
        return device_id;
    }

    public void setDevice_id(String device_id) {
        this.device_id = device_id;
    }

}
