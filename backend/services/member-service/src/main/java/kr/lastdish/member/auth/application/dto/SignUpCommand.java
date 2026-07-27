package kr.lastdish.member.auth.application.dto;

public record SignUpCommand(
    String userName, String password, String name, String phone, String email, String role) {}
