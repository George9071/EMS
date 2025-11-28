package com._6.ems.service;

import com._6.ems.dto.response.ChatMessageDTO;
import com._6.ems.entity.ChatMessage;
import com._6.ems.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;

    @Transactional
    public void saveUserMessage(String personnelCode,
                                String message) {
        chatMessageRepository.save(ChatMessage.builder()
                .personnelCode(personnelCode)
                .message(message)
                .senderType(ChatMessage.SenderType.USER)
                .build());
    }

    @Transactional
    public void saveAssistantMessage(String personnelCode,
                                            String message) {
        chatMessageRepository.save(ChatMessage.builder()
                .personnelCode(personnelCode)
                .message(message)
                .senderType(ChatMessage.SenderType.ASSISTANT)
                .build());
    }

    public List<ChatMessageDTO> getUserChatHistory(String personnelCode) {
        return chatMessageRepository.findByPersonnelCodeOrderByCreatedAtDesc(personnelCode)
                .stream().map(this::toDTO).toList();
    }

    private ChatMessageDTO toDTO(ChatMessage chatMessage) {
        return ChatMessageDTO.builder()
                .id(chatMessage.getId())
                .message(chatMessage.getMessage())
                .senderType(chatMessage.getSenderType().toString())
                .createdAt(chatMessage.getCreatedAt())
                .build();
    }
}