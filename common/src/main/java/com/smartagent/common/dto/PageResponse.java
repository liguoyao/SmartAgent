package com.smartagent.common.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.Serializable;
import java.util.List;

/**
 * Page response DTO for paginated results
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponse<T> implements Serializable {

    private List<T> records;
    private Long total;
    private Integer page;
    private Integer pageSize;
    private Integer totalPages;
    private Boolean hasNext;
    private Boolean hasPrevious;

    public static <T> PageResponse<T> of(List<T> records, Long total, Integer page, Integer pageSize) {
        int totalPages = (int) Math.ceil((double) total / pageSize);
        return PageResponse.<T>builder()
                .records(records)
                .total(total)
                .page(page)
                .pageSize(pageSize)
                .totalPages(totalPages)
                .hasNext(page < totalPages)
                .hasPrevious(page > 1)
                .build();
    }
}
