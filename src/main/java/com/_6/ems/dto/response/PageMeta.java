package com._6.ems.dto.response;
import lombok.*;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PageMeta<T> {
    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int numberOfElements;
    private int page;             // 0-based
    private String sortBy;
    private String sortDir;
}
