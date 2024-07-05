package uz.smartup.academy.bloggingplatform.service;

import uz.smartup.academy.bloggingplatform.dto.LikeDTO;

import java.util.List;

public interface LikeService {

    LikeDTO findByUserAndPost(int userId, int postId);
    void addLike(int userId, int postId);
    void removeLike(int userId, int postId);
    long countLikesByPostId(int postId);
    List<LikeDTO> getAllLikes();

}
