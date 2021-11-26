package ch.heigvd.sprint0.model;
import ch.heigvd.sprint0.repository.ArticleCategoryRepository;
import ch.heigvd.sprint0.repository.CategoryRepository;
import org.hibernate.annotations.Type;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
//The @Entity annotation specifies that the class is an entity and is mapped to a database table while the @Table
// annotation specifies the name of the database table to be used for mapping.

@Entity
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
    private Set<Cart_Article> cart_article_list;

    @OneToMany(mappedBy = "ids.article", cascade = CascadeType.ALL)
    private List<Article_Category> article_category_list;

    public Article(){

    }

    public Article(Integer id, String name, String description, double price, int stock) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setPrixVente(int price) {
        this.price = price;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public void setArticle_category_list(String categoryName) {
        List<Article_Category> articleCategories = new ArrayList<>();
        for(String category : categoryName.split(",")) {
            Category c = new Category(category);
            Article_Category_Ids ids = new Article_Category_Ids(this, c);
            articleCategories.add(new Article_Category(ids));
        }
        this.article_category_list = articleCategories;
    }

    /*public void setArticle_category_list(List<Article_Category> articleCategories) {
        this.article_category_list = articleCategories;
    }*/

    public List<Article_Category> getArticle_category_list() {
        return article_category_list;
    }

    /**
     * Permet de connaître si un article est contenu dans une catégorie
     * @param category la catégorie à analyser
     * @return vrai / faux
     */
    public boolean containsCategory(Category category) {
        for(Article_Category ac : this.getArticle_category_list()) {
            if(ac.getCategory().equals(category))
                return true;
        }
        return false;
    }
}
