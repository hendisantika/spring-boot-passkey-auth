package id.my.hendisantika.passkeyauth.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-passkey-auth
 * User: hendisantika
 * Link: s.id/hendisantika
 * Email: hendisantika@yahoo.co.id
 * Telegram : @hendisantika34
 * Date: 13/04/25
 * Time: 05.44
 * To change this template use File | Settings | File Templates.
 */
@RestController
public class DashboardController {

    @GetMapping("/")
    public String index() {
        return "Hello World";
    }
}
