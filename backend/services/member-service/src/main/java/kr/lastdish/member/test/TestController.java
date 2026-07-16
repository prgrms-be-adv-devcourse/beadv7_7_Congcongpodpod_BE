package kr.lastdish.member.test;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/test")
@Tag(name = "Swagger Test", description = "Swagger 동작 확인용 API")
public class TestController {

    @GetMapping
    @Operation(summary = "GET 테스트", description = "Swagger GET 요청 테스트")
    public ResponseEntity<Map<String, String>> getTest() {
        return ResponseEntity.ok(
                Map.of(
                        "service", "member-service",
                        "method", "GET",
                        "message", "Swagger Test Success!"
                )
        );
    }

    @PostMapping
    @Operation(summary = "POST 테스트", description = "Swagger POST 요청 테스트")
    public ResponseEntity<TestResponse> postTest(@RequestBody TestRequest request) {

        return ResponseEntity.ok(
                new TestResponse(
                        "member-service",
                        request.name(),
                        request.age()
                )
        );
    }

    public record TestRequest(
            String name,
            int age
    ) {}

    public record TestResponse(
            String service,
            String name,
            int age
    ) {}
}