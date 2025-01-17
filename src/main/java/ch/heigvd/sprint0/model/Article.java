package ch.heigvd.sprint0.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
//The @Entity annotation specifies that the class is an entity and is mapped to a database table while the @Table
// annotation specifies the name of the database table to be used for mapping.

@Entity
@Getter
@Setter
@Table(name="Meuble")
public class Article {

    /* Primary key, unique identifier
     */
    //The primary key of an entity is specified with the @Id annotation. The @GeneratedValue gives a strategy for
    // generating the values of primary keys.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    /* article name
     */
    @Column(name = "nom", unique = true)
    private String name;

    /* unique description
     */
    @Column(name = "description", columnDefinition = "text")
    private String description;

    /* article price
     */
    @Column(name = "prixvente", columnDefinition = "DECIMAL(6,2)")
    private double price;

    /* article material
     */
    @Column(name = "quantite")
    private int stock;

    @OneToMany(mappedBy = "ids.article")
    private Set<CartArticle> cart_article_list;

    @OneToMany(mappedBy = "ids.article", cascade = CascadeType.ALL)
    private List<ArticleCategory> article_category_list;

    public Article(){

    }

    public Article(Integer id, String name, String description, double price, int stock) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
    }

    public void setArticleCategoryList(String categoryName) {
        List<ArticleCategory> articleCategories = new ArrayList<>();
        for(String category : categoryName.split(",")) {
            Category c = new Category(category);
            ArticleCategoryIds ids = new ArticleCategoryIds(this, c);
            articleCategories.add(new ArticleCategory(ids));
        }
        this.article_category_list = articleCategories;
    }

    public List<ArticleCategory> getArticleCategoryList() {
        return article_category_list;
    }

    /**
     * Permet de connaître si un article est contenu dans une catégorie
     * @param category la catégorie à analyser
     * @return vrai / faux
     */
    public boolean containsCategory(Category category) {
        for(ArticleCategory ac : this.getArticleCategoryList()) {
            if(ac.getCategory().equals(category))
                return true;
        }
        return false;
    }
}
