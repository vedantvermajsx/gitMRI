-- V1__initial_schema.sql
-- GitHub Repository Intelligence Engine - Initial Schema

CREATE TABLE analysis_jobs (
    id              VARCHAR(36) PRIMARY KEY,
    repo_url        TEXT NOT NULL,
    repo_name       VARCHAR(255),
    branch          VARCHAR(100) DEFAULT 'main',
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    progress        INT DEFAULT 0,
    current_stage   VARCHAR(100),
    error_message   TEXT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    started_at      TIMESTAMP,
    completed_at    TIMESTAMP
);

CREATE TABLE reports (
    id                  VARCHAR(36) PRIMARY KEY,
    job_id              VARCHAR(36) NOT NULL,
    health_score        DECIMAL(5,2),
    total_files         INT DEFAULT 0,
    total_classes       INT DEFAULT 0,
    total_methods       INT DEFAULT 0,
    total_lines         INT DEFAULT 0,
    avg_complexity      DECIMAL(5,2),
    max_complexity      INT DEFAULT 0,
    bus_factor          INT DEFAULT 0,
    dead_code_count     INT DEFAULT 0,
    dead_code_ratio     DECIMAL(5,2),
    hotspot_count       INT DEFAULT 0,
    total_commits       INT DEFAULT 0,
    contributor_count   INT DEFAULT 0,
    dependency_count    INT DEFAULT 0,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (job_id) REFERENCES analysis_jobs(id) ON DELETE CASCADE
);

CREATE TABLE complexity_metrics (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id          VARCHAR(36) NOT NULL,
    file_path       VARCHAR(500),
    package_name    VARCHAR(255),
    class_name      VARCHAR(255),
    method_name     VARCHAR(255),
    cc_score        INT DEFAULT 1,
    line_count      INT DEFAULT 0,
    nesting_depth   INT DEFAULT 0,
    start_line      INT DEFAULT 0,
    end_line        INT DEFAULT 0,
    FOREIGN KEY (job_id) REFERENCES analysis_jobs(id) ON DELETE CASCADE
);

CREATE TABLE dead_code_items (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id          VARCHAR(36) NOT NULL,
    item_type       VARCHAR(20),
    name            VARCHAR(500),
    qualified_name  VARCHAR(1000),
    file_path       VARCHAR(500),
    line_number     INT DEFAULT 0,
    risk_level      VARCHAR(10) DEFAULT 'LOW',
    reason          TEXT,
    FOREIGN KEY (job_id) REFERENCES analysis_jobs(id) ON DELETE CASCADE
);

CREATE TABLE hotspots (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id          VARCHAR(36) NOT NULL,
    file_path       VARCHAR(500),
    commit_count    INT DEFAULT 0,
    last_modified   TIMESTAMP,
    avg_complexity  DECIMAL(5,2),
    hotspot_score   DECIMAL(5,4),
    FOREIGN KEY (job_id) REFERENCES analysis_jobs(id) ON DELETE CASCADE
);

CREATE TABLE contributors (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id          VARCHAR(36) NOT NULL,
    author_name     VARCHAR(255),
    author_email    VARCHAR(255),
    commit_count    INT DEFAULT 0,
    files_owned     INT DEFAULT 0,
    lines_added     INT DEFAULT 0,
    lines_removed   INT DEFAULT 0,
    first_commit    TIMESTAMP,
    last_commit     TIMESTAMP,
    FOREIGN KEY (job_id) REFERENCES analysis_jobs(id) ON DELETE CASCADE
);

CREATE TABLE dependency_nodes (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id          VARCHAR(36) NOT NULL,
    group_id        VARCHAR(255),
    artifact_id     VARCHAR(255),
    version         VARCHAR(100),
    scope           VARCHAR(20) DEFAULT 'compile',
    node_type       VARCHAR(20) DEFAULT 'EXTERNAL',
    FOREIGN KEY (job_id) REFERENCES analysis_jobs(id) ON DELETE CASCADE
);

CREATE TABLE dependency_edges (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id          VARCHAR(36) NOT NULL,
    source_id       BIGINT NOT NULL,
    target_id       BIGINT NOT NULL,
    FOREIGN KEY (job_id) REFERENCES analysis_jobs(id) ON DELETE CASCADE,
    FOREIGN KEY (source_id) REFERENCES dependency_nodes(id) ON DELETE CASCADE,
    FOREIGN KEY (target_id) REFERENCES dependency_nodes(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_jobs_status ON analysis_jobs(status);
CREATE INDEX idx_complexity_job ON complexity_metrics(job_id);
CREATE INDEX idx_dead_code_job ON dead_code_items(job_id);
CREATE INDEX idx_hotspots_job ON hotspots(job_id);
CREATE INDEX idx_contributors_job ON contributors(job_id);
CREATE INDEX idx_dep_nodes_job ON dependency_nodes(job_id);
