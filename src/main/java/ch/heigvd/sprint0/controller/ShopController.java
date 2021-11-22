package ch.heigvd.sprint0.controller;

import ch.heigvd.sprint0.model.Article;
import ch.heigvd.sprint0.service.IArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpSession;
import java.util.List;

@Controller
public class ShopController {
    @Autowired
    private IArticleService articleService;

    @GetMapping("/shop")
    public String findArticles(Model model, HttpSession session) {
        List<Article> articles = articleService.findAll();
        model.addAttribute("articles", articles);
        model.addAttribute("session", session);
        return "shop.html";
    }
}
