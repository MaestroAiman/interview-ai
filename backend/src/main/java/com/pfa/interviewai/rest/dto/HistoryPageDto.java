package com.pfa.interviewai.rest.dto;

import com.pfa.interviewai.model.InterviewSession;

import java.util.List;

public class HistoryPageDto {
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private List<InterviewSession> sessions;

    public HistoryPageDto() {}

    public HistoryPageDto(int page, int size, long totalElements, List<InterviewSession> sessions) {
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        this.sessions = sessions;
    }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public List<InterviewSession> getSessions() { return sessions; }
    public void setSessions(List<InterviewSession> sessions) { this.sessions = sessions; }
}
