package com.space.cloud.assistantlanggraph.controller;

import com.space.cloud.assistantlanggraph.workflow.pet.PetCareAssistant;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PetCareAssistantControllerTest {

    private final PetCareAssistant graphApp = mock(PetCareAssistant.class);

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new PetCareAssistantController(graphApp)).build();

    /**
     * 空白消息在进入工作流之前返回 400。
     */
    @Test
    void rejectsBlankMessageBeforeStartingTheGraph() throws Exception {
        mockMvc.perform(get("/ai/pet/chat").param("message", "   "))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(graphApp);
    }

    /**
     * 空白 chatId 在取消之前返回 400。
     */
    @Test
    void rejectsBlankChatIdWhenCancelling() throws Exception {
        mockMvc.perform(get("/ai/pet/cancel").param("chatId", "   "))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(graphApp);
    }

    /**
     * 取消接口按本次 chatId 停止执行。
     */
    @Test
    void cancelsTheRunningTurnByChatId() throws Exception {
        when(graphApp.cancel("chat-1")).thenReturn(true);

        mockMvc.perform(get("/ai/pet/cancel").param("chatId", "chat-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chatId").value("chat-1"))
                .andExpect(jsonPath("$.cancelled").value(true));

        verify(graphApp).cancel("chat-1");
    }
}
