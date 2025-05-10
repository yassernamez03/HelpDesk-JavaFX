package com.helpdesk.model;

public class KnowledgeBase {
    private int id;
    private String category;
    private String keywords;
    private String problem;
    private String solution;

    public KnowledgeBase(int id, String category, String keywords, String problem, String solution) {
        this.id = id;
        this.category = category;
        this.keywords = keywords;
        this.problem = problem;
        this.solution = solution;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public String getKeywords() {
        return keywords;
    }

    public String getProblem() {
        return problem;
    }

    public String getSolution() {
        return solution;
    }
}