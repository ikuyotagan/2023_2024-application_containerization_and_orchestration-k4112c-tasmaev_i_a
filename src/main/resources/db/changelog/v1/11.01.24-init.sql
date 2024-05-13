--liquibase formatted sql

--changeset app:create_table_tasks
CREATE TABLE tasks (
                       id SERIAL PRIMARY KEY,
                       title VARCHAR(255) NOT NULL,
                       description TEXT,
                       completed BOOLEAN DEFAULT FALSE
);

--changeset your_name:add_index_to_title
CREATE INDEX idx_tasks_title ON tasks (title);
