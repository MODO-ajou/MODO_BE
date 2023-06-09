package com.example.modo_be.book.service;

import com.example.modo_be.book.domain.Book;
import com.example.modo_be.book.exception.BookNotFoundException;
import com.example.modo_be.book.repository.BookRepository;
import com.example.modo_be.book.request.NaverHeaderRequest;
import com.example.modo_be.book.request.PostBookRequest;
import com.example.modo_be.book.response.BookResponse;
import com.example.modo_be.book.response.NaverBookInfo;
import com.example.modo_be.book.response.NaverBookResponse;
import com.example.modo_be.user.domain.User;
import com.example.modo_be.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookService {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    public void postBook(PostBookRequest postBookRequest, String userEmail){
        User user = userRepository.findByUserEmail(userEmail);
        Book newBook = postBookRequest.toEntity();
        newBook.addUser(user);
        bookRepository.save(newBook);

    }

    public List<BookResponse>lendBookList(String userEmail){

        return bookRepository.findAll().stream()
                .filter(book -> book.getUser().getUserEmail().equals(userEmail))
                .map(book -> book.toBookResponse()).collect(Collectors.toList());
    }

    public List<BookResponse>borrowBookList(String userEmail){
        List<Book> bookList = bookRepository.findAll();
        List<BookResponse> bookResponseList = new ArrayList<>();
        bookList.forEach(book -> {if(Objects.equals(book.getBookBorrowUserEmail(), userEmail)){
            bookResponseList.add(book.toBookResponse());
        }});
        return bookResponseList;

    }

    public void returnBook(Long bookId){
        Book book = bookRepository.findById(bookId).orElseThrow(()-> new BookNotFoundException("찾는 책이 없습니다."));
        book.setLoaned(false);
        book.setBookBorrowUserEmail(null);
        bookRepository.save(book);
    }


    public void borrowBook(Long bookId, String userEmail){
        Book book = bookRepository.findById(bookId).orElseThrow(()-> new BookNotFoundException("찾는 책이 없습니다."));
        book.setLoaned(true);
        book.setBookBorrowUserEmail(userEmail);
        bookRepository.save(book);
    }

    public List<BookResponse> getAllBooks(){
        return bookRepository.findAll().stream().filter(book -> !book.isLoaned()).map(Book::toBookResponse).collect(Collectors.toList());
    }

    public List<NaverBookInfo> getNaverBookList(NaverHeaderRequest naverHeaderRequest, String bookTitle) throws JsonProcessingException {

        // 네이버 검색 API 요청
        if(bookTitle==null || bookTitle.equals("")){
            throw new BookNotFoundException("책 제목은 반드시 입력해야 합니다.");
        }

        //String apiURL = "https://openapi.naver.com/v1/search/blog?query=" + bookTitle;    // JSON 결과
        URI uri = UriComponentsBuilder
                .fromUriString("https://openapi.naver.com")
                .path("/v1/search/book.json")
                .queryParam("query", bookTitle)
                .queryParam("start", 1)
                .queryParam("sort", "sim")
                .encode()
                .build()
                .toUri();

        // Spring 요청 제공 클래스
        RequestEntity<Void> req = RequestEntity
                .get(uri)
                .header("X-Naver-Client-Id", naverHeaderRequest.getClientId())
                .header("X-Naver-Client-Secret", naverHeaderRequest.getClientSecret())
                .build();


        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> resp = restTemplate.exchange(req, String.class);

        // JSON 파싱 (Json 문자열을 객체로 만듦, 문서화)
        ObjectMapper om = new ObjectMapper();
        NaverBookResponse naverBookResponse = null;


        try {
            return om.readValue(resp.getBody(), NaverBookResponse.class).getItems();
        } catch (JsonMappingException e) {
            throw JsonMappingException.fromUnexpectedIOE(e); // -> JsonProcessingException -> IOException
        } catch (JsonProcessingException e) {
            throw e; // exception handler ?
        } catch (NullPointerException e){
            throw new NullPointerException();
        }

    }

}
