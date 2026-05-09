package com.smartagent.common.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.Serializable;
import java.util.List;

/**
 * Page request DTO for pagination
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageRequest implements Serializable {

    private Integer page = 1;
    private Integer pageSize = 10;
    private String sortBy;
    private String sortOrder = "desc";

    public int getOffset() {
        return (page - 1) * pageSize;
    }

    public int getLimit() {
        return pageSize;
    }
}
