package com.prossafy101.ohnaco.service;

import com.prossafy101.ohnaco.entity.tech.Article;
import com.prossafy101.ohnaco.entity.tech.ArticleDto;
import com.prossafy101.ohnaco.entity.tech.Blog;
import com.prossafy101.ohnaco.entity.tech.BlogDto;
import com.prossafy101.ohnaco.entity.user.User;
import com.prossafy101.ohnaco.repository.ArticleRepository;
import com.prossafy101.ohnaco.repository.BlogRepository;
import com.prossafy101.ohnaco.repository.UserRepository;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import springfox.documentation.swagger2.mappers.ModelMapper;

import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class TechService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BlogRepository blogRepository;

    @Autowired
    private ArticleRepository articleRepository;


    public List<BlogDto> getBlogList(String userid) {

        List<Blog> list = blogRepository.findAll();
        List<BlogDto> dtoList = new ArrayList<>();
        for (Blog b : list) {
            dtoList.add(BlogDto.builder()
                    .blogid(b.getBlogid())
                    .blogname(b.getBlogname())
                    .description(b.getDescription())
                    .link(b.getLink())
                    .issubscribe(b.getUsers().contains(userRepository.findByUserid(userid))).build());
        }

        return dtoList;
    }

    public List<BlogDto> getSubscribeBlogList(String userid) {

        List<Blog> list = blogRepository.findAll();
        List<BlogDto> dtoList = new ArrayList<>();
        for (Blog b : list) {
            if (b.getUsers().contains(userRepository.findByUserid(userid))) {
                dtoList.add(BlogDto.builder()
                        .blogid(b.getBlogid())
                        .blogname(b.getBlogname())
                        .description(b.getDescription())
                        .link(b.getLink())
                        .issubscribe(true).build());
            }
        }

        return dtoList;
    }

    public void subscribeBlog(String userid, Long blogid) {

        Blog blog = blogRepository.findById(blogid).get();
        List<User> users = blog.getUsers();
        users.add(userRepository.findByUserid(userid));

        blogRepository.save(Blog.builder()
                .blogid(blog.getBlogid())
                .rss(blog.getRss())
                .users(users)
                .blogname(blog.getBlogname())
                .description(blog.getDescription())
                .link(blog.getLink())
                .build());
    }

    public void cancelSubscribe(String userid, Long blogid) {
        Blog blog = blogRepository.findById(blogid).get();
        List<User> users = blog.getUsers();
        users.remove(userRepository.findByUserid(userid));

        blogRepository.save(Blog.builder()
                .blogid(blog.getBlogid())
                .rss(blog.getRss())
                .users(users)
                .blogname(blog.getBlogname())
                .description(blog.getDescription())
                .link(blog.getLink())
                .build());
    }

    public Page<Article> getAllArticles(Pageable pageable, String keyword) {
        if (keyword.equals("") && keyword.equals("")) {
            return articleRepository.findAll(pageable);
        } else if (!keyword.equals("")) {
            return articleRepository.findAllByTitleOrContentContaining(keyword, keyword, pageable);
        }
        return null;
    }

    public Page<Article> getSubscribeArticles(Pageable pageable, String userid)throws Exception  {

       try {
            return articleRepository.findArticles(userid, pageable);
        } catch(Exception e) {
            throw new Exception("");
        }

    }

    public List<Article> getFindArticleId(List<Long> articleids) {
        return articleRepository.findAllByArticleids(articleids);
    }

    public boolean isSubscribe(String userid, Long blogid) {
        Blog blog = blogRepository.findById(blogid).get();
        List<User> users = blog.getUsers();
        return users.stream().filter(user -> user.getUserid().equals(userid)).findFirst().isPresent();
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void getRSSArticles() {

        List<Blog> blogs = blogRepository.findAll();

        try {
            for (Blog blog : blogs) {
                URL feedUrl = new URL(blog.getRss());
                SyndFeedInput input = new SyndFeedInput();
                SyndFeed feed = input.build(new XmlReader(feedUrl));

                List entries = feed.getEntries();
                for (int i = 0; i < entries.size(); i++) {

                    SyndEntry entry = (SyndEntry) entries.get(i);
                    if(blog.getBlogname().equals("coupang"))
                       if(!entry.getCategories().get(0).getName().equals("tech-notes"))
                           continue;
                    Article article = Article.builder()
                            .blogid(blog.getBlogid())
                            .image(blog.getBlogname())
                            .title(entry.getTitle())
                            .content(entry.getDescription()==null? StringEscapeUtils.unescapeHtml4(entry.getContents().get(0).getValue()).replaceAll("<[^>]*>","").substring(0, 180):StringEscapeUtils.unescapeHtml4(entry.getDescription().getValue()).replaceAll("<[^>]*>","").substring(0, 180))
                            .link(entry.getLink())
                            .publisheddate(entry.getPublishedDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())
                            .build();

                    if (articleRepository.findArticleByLink(entry.getLink()) == null)
                        articleRepository.save(article);
                }
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (FeedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
