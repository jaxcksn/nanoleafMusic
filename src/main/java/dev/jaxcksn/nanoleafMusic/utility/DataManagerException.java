/*
 * Copyright (c) 2020, Jaxcksn
 * All rights reserved.
 */

package dev.jaxcksn.nanoleafMusic.utility;

public class DataManagerException extends RuntimeException {
    public dMEC code;

    public DataManagerException(String errorMessage, dMEC code) {
        super(errorMessage);
        this.code = code;
    }
}
