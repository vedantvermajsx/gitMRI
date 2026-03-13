package com.repointel.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "dead_code_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeadCodeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false, length = 36)
    private String jobId;

    @Column(name = "item_type", length = 20)
    private String itemType; // CLASS, METHOD, FIELD

    @Column(name = "name", length = 500)
    private String name;

    @Column(name = "qualified_name", length = 1000)
    private String qualifiedName;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "line_number")
    private int lineNumber;

    @Column(name = "risk_level", length = 10)
    private String riskLevel; // HIGH, MEDIUM, LOW

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;
}
