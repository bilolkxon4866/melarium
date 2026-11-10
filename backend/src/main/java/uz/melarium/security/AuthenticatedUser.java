package uz.melarium.security;

import java.util.List;
import java.util.UUID;

public record AuthenticatedUser(UUID id, List<String> roles) { }
