package com.example.sales.model;

import com.example.sales.model.base.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("categories")
public class Category extends BaseEntity {
    @Id
    private String id;

    @NotBlank(message = "Tên danh mục không được để trống")
    private String name;

    private Map<String, String> nameTranslations;

    @NotBlank(message = "Shop ID không được để trống")
    private String shopId;

    private String description;

    @Builder.Default
    private boolean active = true;
}