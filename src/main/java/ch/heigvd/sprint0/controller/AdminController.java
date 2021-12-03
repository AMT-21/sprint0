package ch.heigvd.sprint0.controller;

import ch.heigvd.sprint0.model.Article;
import ch.heigvd.sprint0.model.Article_Category;
import ch.heigvd.sprint0.model.Article_Category_Ids;
import ch.heigvd.sprint0.model.Category;
import ch.heigvd.sprint0.repository.ArticleCategoryRepository;
import ch.heigvd.sprint0.repository.CategoryRepository;
import ch.heigvd.sprint0.service.IArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Controller
public class AdminController {

    @Autowired
    private IArticleService articleService;
    @Autowired
    private ArticleCategoryRepository articleCategoryRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Value("${server.tomcat.upload-dir}")
    private String uploadPath;

    @GetMapping("/admin")
    public String admin(Model model) {
        List<Article> articles = articleService.findAll();

        model.addAttribute("articles", articles);
        return "admin.html";
    }

    @GetMapping("/admin/article")
    public String adminArticle(Model model, @RequestParam(name = "id", required = false) String id,
                               @RequestParam(name = "error", required = false) String error,
                               @RequestParam(name = "error_msg", required = false) String errorMsg) {
        if(id != null && !id.chars().allMatch(Character::isDigit))
            return "redirect:/admin";

        Article modelArticle = null;
        if(id != null) {
            Optional<Article> article = articleService.findById(Integer.parseInt(id));
            if(article.isPresent()) {
                modelArticle = article.get();
            }
        }

        if(error != null) {
            if ("DescAlreadyUsed".equals(error)) {
                error = "Cette description d'article est déjà utilisée par l'article " + errorMsg;
            }
            if ("NotAnImage".equals(error)) {
                error = "L'image n'est pas valide";
            }
        }

        List<Category> categories = (List<Category>) categoryRepository.findAll();
        if(modelArticle != null)
            model.addAttribute("article", modelArticle); // article qui se trouve dans l'url
        else
            model.addAttribute("article", new Article()); // article vide qui va être remplit dans le formulaire
        model.addAttribute("categories", categories);
        model.addAttribute("error", error);
        return "adminArticle.html";
    }

    @PostMapping("/admin/article")
    public String articleSubmit(@ModelAttribute Article article, Model model,
                                @RequestParam(value = "image", required = false) MultipartFile image,
                                @RequestParam(value = "article_category_list", required = false) String article_category_list) {

        Optional<Article> articleWithSameDescription = articleService.findByDescription(article.getDescription());
        // C'est une modification d'article
        if(article.getId() != null) {
            // Vérifier que la description n'est pas déjà utilisée par un autre article
            if(articleWithSameDescription.isPresent() && articleWithSameDescription.get().getId() != article.getId()) {
                return "redirect:/admin/article?error=DescAlreadyUsed&error_msg=" + articleWithSameDescription.get().getName();
            }

            if(article_category_list == null) {
                // aucune catégorie cochée, on les supprime tous si elles existent
                for(Article_Category articleAc : articleCategoryRepository.findArticle_CategoriesByIds_Article(article)) {
                    Article_Category_Ids ids = new Article_Category_Ids(article, articleAc.getCategory());
                    articleCategoryRepository.deleteById(ids);
                }
            } else {
                // supprimer les catégories qui ne sont pas cochées
                List<Category> allCats = (List<Category>) categoryRepository.findAll();
                String[] articleCats = article_category_list.split(",");
                for(Category c : allCats) {
                    // si la catégorie n'appartient pas à l'article
                    if(!Arrays.asList(articleCats).contains(c.getNameCategory())) {
                        // si la catégorie est une ancienne catégorie de l'article
                        Category formerCat = new Category(c.getNameCategory());
                        Article_Category_Ids ids = new Article_Category_Ids(article, formerCat);

                        for(Article_Category articleAc : articleCategoryRepository.findArticle_CategoriesByIds_Article(article)) {
                            if(articleAc.getCategory().getNameCategory().equals(c.getNameCategory())) {
                                articleCategoryRepository.deleteById(ids);
                            }
                        }
                    }
                }
            }

        // C'est un ajout d'article
        } else {
            // Vérifier que la description n'est pas déjà utilisée par un autre article
            if(articleWithSameDescription.isPresent()) {
                return "redirect:/admin/article?error=DescAlreadyUsed&error_msg=" + articleWithSameDescription.get().getName();
            }
        }

        articleService.saveArticle(article);

        if(!image.isEmpty()) {
            // On accepte que les images
            try (InputStream input = image.getInputStream()) {
                try { ImageIO.read(input).toString(); } catch (Exception e) {
                    // It's not an image.
                    return "redirect:/admin/article?error=NotAnImage";
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Récupère l'id du nouvel élément pour l'utiliser comme nom d'image
            Article insertedArticle = article.getId() != null ? article : articleService.findTopByOrderByIdDesc().get(0);
            // Upload de l'image
            try {
                String extension = "." + image.getOriginalFilename().split("\\.")[image.getOriginalFilename().split("\\.").length - 1];
                // On accepte que les .jpg
                if(!extension.equals(".jpg")) {
                    return "redirect:/admin/article?error=NotAnImage";
                }

                if (!Files.exists(Paths.get(uploadPath))) {
                    Files.createDirectories(Paths.get(uploadPath));
                }

                Path destinationFile = Paths.get(uploadPath).resolve(
                                Paths.get(insertedArticle.getId().toString() + extension)).normalize().toAbsolutePath();

                try (InputStream inputStream = image.getInputStream()) {
                    Files.copy(inputStream, destinationFile,
                            StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return "redirect:/admin";
    }

    @GetMapping("/admin/article/delete")
    public String articleDelete(Model model, @RequestParam(name = "id") String id) {
        // L'id existe ?
        Optional<Article> article = articleService.findById(Integer.parseInt(id));
        article.ifPresent(value -> articleService.deleteArticle(value));

        return "redirect:/admin";
    }

    @GetMapping("/admin/categories")
    public String adminCategories(Model model,
                                  @RequestParam(name = "error", required = false) String error) {
        List<Category> categories = (List<Category>) categoryRepository.findAll();
        if(!categories.isEmpty()) {
            model.addAttribute("categories", categories);
        }

        model.addAttribute("category", new Category());
        model.addAttribute("error", error);
        return "adminCategories.html";
    }

    @PostMapping("/admin/categories")
    public String categorySubmit(@ModelAttribute Category category, Model model) {
        // Vérifier que le nom de la catégorie existe pas déjà
        Optional<Category> cat = categoryRepository.findById(category.getNameCategory());
        if(cat.isPresent()) {
            return "redirect:/admin/categories?error=catAlreadyExists";
        }

        categoryRepository.save(new Category(category.getNameCategory()));
        return "redirect:/admin/categories";
    }

    @GetMapping("/admin/categories/confirmDeletion")
    public String confirmCategoryDeletion(Model model, @RequestParam(name = "id") String id) {
        // L'id existe ?
        Optional<Category> category = categoryRepository.findById(id);
        if(category.isPresent()) {
            // Récupérer la liste des articles impactés
            List<Article_Category> acs = articleCategoryRepository.findArticle_CategoriesByIds_Category(new Category(id));
            if(acs.isEmpty()) {
                categoryRepository.delete(new Category(id));
                return "redirect:/admin/categories";
            }
            List<Article> concernedArticles = new ArrayList<>();
            for(Article_Category ac : acs) {
                concernedArticles.add(ac.getArticle());
            }

            model.addAttribute("articles", concernedArticles);
            model.addAttribute("category", category.get());
            return "adminCategoriesConfirmDeletion.html";
        }

        return "redirect:/admin/categories";
    }

    @GetMapping("/admin/categories/delete")
    public String categoryDelete(Model model, @RequestParam(name = "id") String id) {
        // L'id existe ?
        Optional<Category> category = categoryRepository.findById(id);
        if(category.isPresent()) {
            categoryRepository.delete(new Category(id));
        }

        return "redirect:/admin/categories";
    }

}
