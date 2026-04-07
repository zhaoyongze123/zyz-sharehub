package com.sharehub.me;

import com.sharehub.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final MeService meService;

    public MeController(MeService meService) {
        this.meService = meService;
    }

    @GetMapping
    public ApiResponse<MeDto> getMe() {
        return ApiResponse.ok(meService.aggregate("local-dev-user"));
    }
}
