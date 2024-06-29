package uz.smartup.academy.bloggingplatform.service;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import uz.smartup.academy.bloggingplatform.dao.PostDao;
import uz.smartup.academy.bloggingplatform.dto.CommentDTO;
import uz.smartup.academy.bloggingplatform.dto.CommentDtoUtil;
import uz.smartup.academy.bloggingplatform.dto.PostDto;
import uz.smartup.academy.bloggingplatform.dto.PostDtoUtil;
import uz.smartup.academy.bloggingplatform.entity.Comment;
import uz.smartup.academy.bloggingplatform.entity.Post;
import uz.smartup.academy.bloggingplatform.entity.User;

import java.util.List;

@Service
public class PostServiceImpl implements PostService {
    private final PostDao dao;

    private final PostDtoUtil dtoUtil;
    private final CommentDtoUtil commentDtoUtil;
    private final LikeService likeService;

    public PostServiceImpl(PostDao dao, PostDtoUtil dtoUtil, CommentDtoUtil commentDtoUtil, LikeService likeService) {
        this.dao = dao;
        this.dtoUtil = dtoUtil;
        this.commentDtoUtil = commentDtoUtil;
        this.likeService = likeService;
    }

    @Override
    @Transactional
    public void createPost(Post post) {
        dao.save(post);
    }

    @Override
    @Transactional
    public void update(PostDto postDto) {
        Post post = dtoUtil.toEntity(postDto);

        post.setComments(dao.getPostComments(post.getId()));
        post.setAuthor(dao.getAuthorById(post.getId()));
        post.setStatus(dao.findPostStatusById(post.getId()));

        dao.update(post);
    }

    @Override
    @Transactional
    public void delete(int postId) {
        dao.delete(dao.getById(postId));
    }

    @Override
    public PostDto getById(int id) {
        return dtoUtil.toDto(dao.getById(id));
    }

    @Override
    public List<PostDto> getAllPosts() {
        return dtoUtil.toDTOs(dao.getAllPosts());
    }

    @Override
    public List<PostDto> getPostsByTag(int tagId) {
        return dtoUtil.toDTOs(dao.getPostsByTag(tagId));
    }

    @Override
    public List<PostDto> getPostsByCategory(int categoryId) {
        return dtoUtil.toDTOs(dao.getPostsByCategory(categoryId));
    }

    @Override
    public User getAuthorById(int id) {
        return dao.getAuthorById(id);
    }

    @Override
    public List<PostDto> getPostsByAuthor(int authorId) {
        return dtoUtil.toDTOs(dao.getPostsByAuthor(authorId));
    }

    @Override
    public List<CommentDTO> getPostComments(int id) {
        List<Comment> comments = dao.getPostComments(id);

        return commentDtoUtil.toDTOs(comments);
    }

    @Override
    public List<PostDto> getDraftPost() {
        List<Post> posts = dao.findPostsByStatus(Post.Status.DRAFT);

        return dtoUtil.toDTOs(posts);
    }

    @Override
    public List<PostDto> getPublishedPost() {
        List<Post> posts = dao.findPostsByStatus(Post.Status.PUBLISHED);

        return dtoUtil.toDTOs(posts);
    }

    @Override
    public List<PostDto> getDraftPostsByAuthorId(int authorId) {
        List<Post> posts = dao.findPostByStatusAndAuthorId(Post.Status.DRAFT, authorId);

//        System.out.println(dtoUtil.toDTOs(posts));
        return dtoUtil.toDTOs(posts);
    }

    @Override
    public List<PostDto> getPublishedPostsByAuthorId(int authorId) {
        List<Post> posts = dao.findPostByStatusAndAuthorId(Post.Status.PUBLISHED, authorId);

        System.out.println(dtoUtil.toDTOs(posts));

        return dtoUtil.toDTOs(posts);
    }

    @Override
    public Post getPostWithLikeCount(int postId) {
        Post post = dtoUtil.toEntity(getById(postId));
        long likeCount = likeService.countLikesByPostId(postId);
        post.setLikesCount(likeCount);
        return post;
    }



    @Override
    @Transactional
    public void switchPostDraftToPublished(int id) {
        Post post = dao.getById(id);

        post.setStatus(Post.Status.PUBLISHED);

        dao.update(post);
    }

    @Override
    @Transactional
    public void switchPublishedToDraft(int id) {
        Post post = dao.getById(id);

        post.setStatus(Post.Status.DRAFT);

        dao.update(post);
    }
}
