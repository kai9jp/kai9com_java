package kai9.com.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping("/")
    private String index() {
        return "/index.html";
    }

    @GetMapping("/test")
    private String test() {
        return "test.html";
    }

}
