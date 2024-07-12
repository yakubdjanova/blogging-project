package uz.smartup.academy.bloggingplatform.mvc;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uz.smartup.academy.bloggingplatform.config.CategoryConfiguration;
import uz.smartup.academy.bloggingplatform.dto.CategoryDto;
import uz.smartup.academy.bloggingplatform.dto.CommentDTO;
import uz.smartup.academy.bloggingplatform.dto.PostDto;
import uz.smartup.academy.bloggingplatform.dto.UserDTO;
import uz.smartup.academy.bloggingplatform.entity.Post;
import uz.smartup.academy.bloggingplatform.entity.Role;
import uz.smartup.academy.bloggingplatform.service.CategoryService;
import uz.smartup.academy.bloggingplatform.service.LikeService;
import uz.smartup.academy.bloggingplatform.service.PostService;
import uz.smartup.academy.bloggingplatform.service.UserService;

import java.io.IOException;
import java.util.List;

@Controller
public class IndexController {
    private final PostService postService;
    private final CategoryService categoryService;
    private final UserService userService;
    private final LikeService likeService;
    private final CategoryConfiguration categoryConfiguration;

    public IndexController(PostService postService, CategoryService categoryService, UserService userService, LikeService likeService, CategoryConfiguration categoryConfiguration) {
        this.postService = postService;
        this.categoryService = categoryService;
        this.userService = userService;
        this.likeService = likeService;
        this.categoryConfiguration = categoryConfiguration;
    }

    @GetMapping("/")
    public String index(Model model) {
        List<PostDto> posts = postService.getPublishedPost();

        if (posts != null) {
            posts = posts.stream()
                    .filter(postDto -> postDto.getStatus().equals(Post.Status.PUBLISHED))
                    .sorted((post1, post2) -> post2.getCreatedAt().compareTo(post1.getCreatedAt()))
                    .toList();

            if (posts.size() > 20) {
                posts = posts.stream()
                        .limit(20)
                        .toList();
            }

            for (PostDto post : posts) {
                post.setLikesCount(likeService.countLikesByPostId(post.getId()));
            }

            if(getLoggedUser() != null)
                for(PostDto postDto : posts)
                    postDto.setLiked(likeService.findByUserAndPost(userService.getUserByUsername(getLoggedUser().getUsername()).getId(), postDto.getId()) != null);
            else
                for (PostDto postDto : posts)
                    postDto.setLiked(false);

        }
        String photo = "";
        UserDTO userDTO = getLoggedUser() == null ? null : userService.getUserByUsername(getLoggedUser().getUsername());
        if(userDTO != null){
            //System.out.println(userDTO.getRoles());
            photo = userService.encodePhotoToBase64(userDTO.getPhoto());
            List<Role>roles = userService.userFindByRoles(userDTO.getUsername());
            boolean isAdmin = roles.stream()
                    .anyMatch(role -> "ROLE_ADMIN".equals(role.getRole()));
            if(isAdmin)return "redirect:/admin";
        }

        List<CategoryDto> categories = categoryService.getAllCategories();

        PostDto topPost = (posts != null && !posts.isEmpty()) ? posts.getFirst() : null;

        model.addAttribute("topPost", topPost);
        model.addAttribute("posts", posts);
        model.addAttribute("photo", photo);
        model.addAttribute("categories", categories);
        model.addAttribute("loggedIn", getLoggedUser());
        return "index";
    }

    @GetMapping("/posts/{postId}")
    public String getPostById(@PathVariable("postId") int postId, Model model) {
        PostDto post = postService.getById(postId);
        post.setLikesCount(likeService.countLikesByPostId(postId));
        List<CommentDTO> comments = postService.getPostComments(postId);
        List<CategoryDto> categories = categoryService.getAllCategories();
        post.setLikesCount(likeService.countLikesByPostId(postId));


        comments.forEach(commentDTO -> commentDTO.setUsername(userService.getUserById(commentDTO.getAuthorId()).getUsername()));
        String photo = "";
        UserDTO userDTO = getLoggedUser() == null ? null : userService.getUserByUsername(getLoggedUser().getUsername());
        if(userDTO != null)
            photo = userService.encodePhotoToBase64(userDTO.getPhoto());

        if(getLoggedUser() != null)
            post.setLiked(likeService.findByUserAndPost(userService.getUserByUsername(getLoggedUser().getUsername()).getId(), post.getId()) != null);


        model.addAttribute("photo", photo);
        model.addAttribute("loggedIn", getLoggedUser());
        model.addAttribute("commentsSize", comments.size());
        model.addAttribute("post", post);
        model.addAttribute("comments", comments);
        model.addAttribute("categories", categories);
        model.addAttribute("newComment", new CommentDTO());
        model.addAttribute("loggedIn", getLoggedUser());

        return "getPost";
    }

    @PostMapping("/posts/{postId}/submitComment/{username}")
    public String createComment(RedirectAttributes attributes, @PathVariable("postId") int postId, @PathVariable("username") String username, @ModelAttribute("newComment") CommentDTO comment) {
        postService.addCommentToPost(userService.getUserByUsername(username).getId(), postId, comment);


        attributes.addAttribute("id", postId);

        return "redirect:/posts/{id}";
    }

    @PostMapping("/posts/{postId}/likes/{username}")
    public String createLike(@PathVariable("postId") int postId, @PathVariable("username") String username) {
        likeService.addLike(userService.getUserByUsername(username).getId(), postId);

        return "redirect:/";
    }

    @PostMapping("/{postId}/likes/{username}")
    public String likePost(@PathVariable("postId") int postId, @PathVariable("username") String username, RedirectAttributes attributes) {
        likeService.addLike(userService.getUserByUsername(username).getId(), postId);
        PostDto postDto = postService.getById(postId);

        postDto.setLiked(likeService.findByUserAndPost(userService.getUserByUsername(username).getId(), postId) != null);

        attributes.addAttribute("postId", postId);

        return "redirect:/posts/{postId}";
    }

    @PostMapping("categories/{categoryTitle}/{postId}/likes/{username}")
    public String likeCategory(@PathVariable("categoryTitle") String categoryTitle, @PathVariable("postId") int postId, @PathVariable("username") String username, RedirectAttributes attributes) {
        likeService.addLike(userService.getUserByUsername(username).getId(), postId);

        attributes.addAttribute("categoryTitle", categoryTitle);

        return "redirect:/categories/{categoryTitle}";
    }

    @GetMapping("/categories/{categoryTitle}")
    public String categoryPost(@PathVariable("categoryTitle") String categoryTitle, Model model) {
        List<PostDto> posts = postService.getPostsByCategory(categoryTitle);

        if (posts != null) {
            posts = posts.stream()
                    .filter(postDto -> postDto.getStatus().equals(Post.Status.PUBLISHED))
                    .sorted((post1, post2) -> post2.getCreatedAt().compareTo(post1.getCreatedAt()))
                    .toList();

            if (posts.size() > 20) {
                posts = posts.stream()
                        .limit(20)
                        .toList();
            }

            for (PostDto post : posts) {
                post.setLikesCount(likeService.countLikesByPostId(post.getId()));
            }

            if(getLoggedUser() != null)
                for(PostDto postDto : posts)
                    postDto.setLiked(likeService.findByUserAndPost(userService.getUserByUsername(getLoggedUser().getUsername()).getId(), postDto.getId()) != null);
            else
                for (PostDto postDto : posts)
                    postDto.setLiked(false);
        }

        String photo = "";
        UserDTO userDTO = getLoggedUser() == null ? null : userService.getUserByUsername(getLoggedUser().getUsername());
        if(userDTO != null)
            photo = userService.encodePhotoToBase64(userDTO.getPhoto());

        List<CategoryDto> categories = categoryService.getAllCategories();

        PostDto topPost = (posts != null && !posts.isEmpty()) ? posts.getFirst() : null;

        model.addAttribute("loggedIn", getLoggedUser());
        model.addAttribute("photo", photo);
        model.addAttribute("posts", posts);
        model.addAttribute("topPost", topPost);
        model.addAttribute("categories", categories);
        model.addAttribute("categoryTitle", categoryTitle);

        return "categoryPosts";
    }

    @GetMapping("/profile/{username}")
    public String profile(@PathVariable("username") String username, Model model) {
        UserDTO user = userService.getUserByUsername(username);
        List<CategoryDto> categories = categoryService.getAllCategories();

        String base64EncodedPhoto = userService.encodePhotoToBase64(user.getPhoto());
        model.addAttribute("photo", base64EncodedPhoto);
        model.addAttribute("categories", categories);
        model.addAttribute("user", user);

        return "profile";
    }

    @PostMapping("/profile/{username}/uploadPhoto")
    public String uploadPhoto(RedirectAttributes attributes,@RequestParam("file") MultipartFile file, Model model, @PathVariable("username") String username) {
        UserDTO user = userService.getUserByUsername(username);

        try {
            byte[] bytes = file.getBytes();
            if(!file.isEmpty() && file != null) {
                user.setPhoto(bytes);
                userService.updateUser(user);
            }
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("errorMessage", "Failed to upload the photo. Please try again.");
        }

        attributes.addAttribute("username", username);

        return "redirect:/profile/{username}";
    }


    @GetMapping("/profile/{userId}/edit")
    public String editProfile(Model model, @PathVariable("userId") String  username) {
        UserDTO user = userService.getUserByUsername(username);
        List<CategoryDto> categories = categoryService.getAllCategories();

        String base64EncodedPhoto = userService.encodePhotoToBase64(user.getPhoto());
        model.addAttribute("base64EncodedPhoto", base64EncodedPhoto);
        model.addAttribute("user", user);
        model.addAttribute("categories", categories);

        return "editUser";
    }

    @PostMapping("/profile/{userId}/update")
    public String updateUser(@PathVariable("userId") int userId,Model model, @ModelAttribute("user") UserDTO userDTO, RedirectAttributes attributes, @RequestParam(value = "file", required = false) MultipartFile photo) throws IOException {
        try {
//            System.out.println(userDTO.getId());
            UserDTO user = userService.getUserById(userId);
            if (photo == null || photo.isEmpty()) {
                userDTO.setPhoto(user.getPhoto());
            }else {
                byte[] photoBytes = photo.getBytes();
                userDTO.setPhoto(photoBytes);
            }
            userDTO.setId(user.getId());
            userDTO.setUsername(user.getUsername());
            userDTO.setEmail(user.getEmail());
            userDTO.setPassword(user.getPassword());
            userDTO.setRegistered(user.getRegistered());
            userService.updateUser(userDTO);
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("errorMessage", "Failed to update the profile. Please try again.");
        }

        attributes.addAttribute("username", userDTO.getUsername());

        return "redirect:/profile/{username}";
    }


    @PostMapping("/web/posts/create")
    public String CreatePostController(@ModelAttribute("post") PostDto postDto, Model model){
        model.addAttribute("categories", categoryConfiguration.getCategories());
        return "createPost";
    }

    @GetMapping("/web/posts/create")
    public String CreatePostController(Model model){
        model.addAttribute("categories", categoryConfiguration.getCategories());
        return "createPost";
    }

    private UserDetails getLoggedUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof UserDetails)
            return (UserDetails) principal;

        return null;
    }

    @PostMapping("/deletePhoto/{userId}")
    public String deletePhoto(@PathVariable("userId") int userId, RedirectAttributes attributes) {
        UserDTO userDTO = userService.getUserById(userId);

        userService.setDefaultPhotoToUser(userDTO);

        attributes.addAttribute("username", userDTO.getUsername());

        return "redirect:/profile/{username}";
    }

}