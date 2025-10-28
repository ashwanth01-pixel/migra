package com.ust.gitproxy.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author Valeriy Kucherenko
 * @since 14.11.2022
 */
@Data
@Schema(implementation = String.class)
public class Credentials {
    public static final String HEADER = "X-Credentials";

    private String git;
    private String proxy;
}
