CREATE TABLE phuc_db.spring_ai_chat_memory (
                                               conversation_id VARCHAR(36) NOT NULL,
                                               content TEXT NOT NULL,
                                               type VARCHAR(10) NOT NULL,
                                               timestamp TIMESTAMP NOT NULL,
                                               PRIMARY KEY (conversation_id, timestamp),
                                               CONSTRAINT spring_ai_chat_memory_type_check
                                                   CHECK (type IN ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL'))
);

CREATE INDEX spring_ai_chat_memory_conversation_id_timestamp_idx
    ON phuc_db.spring_ai_chat_memory (conversation_id, timestamp);