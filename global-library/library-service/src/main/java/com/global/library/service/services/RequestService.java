package com.global.library.service.services;

import com.global.library.api.dao.IBookDao;
import com.global.library.api.dao.IRequestDao;
import com.global.library.api.dao.IUserDao;
import com.global.library.api.dto.RequestDto;
import com.global.library.api.enums.RequestStatusName;
import com.global.library.api.mappers.RequestMapper;
import com.global.library.api.services.IRequestService;
import com.global.library.api.utils.IEmailSendler;
import com.global.library.entity.Book;
import com.global.library.entity.Request;
import com.global.library.entity.User;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RequestService implements IRequestService {

    private final IRequestDao requestDao;
    private final IBookDao bookDao;
    private final IUserDao userDao;
    private final IEmailSendler emailSendler;

    public RequestService(IRequestDao requestDao, IBookDao bookDao, IUserDao userDao, IEmailSendler emailSendler) {
        this.requestDao = requestDao;
        this.bookDao = bookDao;
        this.userDao = userDao;
        this.emailSendler = emailSendler;
    }

    @Override
    @Transactional
    public void createRequest(String isbn, String email) {
        Request request = new Request();
        Book book = this.bookDao.findBookByIsbn(isbn);
        User user = this.userDao.findUserByEmail(email);
        request.setBook(book);
        request.setUser(user);
        request.setDateOfCreation(LocalDateTime.now());
        request.setStatus(RequestStatusName.CREATED.getNameDB());
        book.getRequests().add(request);
        user.getRequests().add(request);
        this.requestDao.create(request);
    }

    @Override
    @Transactional
    public void deleteRequest(long id) {
        this.requestDao.delete(this.requestDao.get(id));
    }

    @Override
    @Transactional
    public List<RequestDto> getAllCreatedRequestsFromUserByEmail(String email) {
        return RequestMapper.mapAllRequestsDto(this.requestDao.findAllCreatedRequestsFromUserByEmail(email));
    }

    @Override
    @Transactional
    public List<RequestDto> getAllConfirmedRequestsFromUserByEmail(String email) {
        return RequestMapper.mapAllRequestsDto(this.requestDao.findAllConfirmedRequestsFromUserByEmail(email));
    }

    @Override
    @Transactional
    public List<RequestDto> getAllRequests(String status) {
        return RequestMapper.mapAllRequestsDto(this.requestDao.findAllRequests(status));
    }

    @Override
    @Transactional
    public List<RequestDto> getAllRequestsBySearch(String status, String search) {
        return RequestMapper.mapAllRequestsDto(this.requestDao.findAllRequestsBySearch(status, search));
    }

    @Override
    @Transactional
    public boolean isRequestExistForCurrentBookFromUser(String isbn, String email) {
        return this.requestDao.isRequestExistForCurrentBookFromUser(isbn, email);
    }

    @Override
    @Transactional
    public void confirmRequests(List<RequestDto> requestDtos) {
        for (RequestDto requestDto : requestDtos) {
            Request request = RequestMapper.mapRequest(requestDto);
            request.setStatus(RequestStatusName.CONFIRMED.getNameDB());
            this.requestDao.update(request);
        }
    }

    @Override
    @Transactional
    public void processRequest(long id) {
        Request request = this.requestDao.get(id);
        request.setStatus(RequestStatusName.PROCESSED.getNameDB());
        request.setDateOfExtradition(LocalDateTime.now());
        this.requestDao.update(request);
    }

    @Override
    @Transactional
    public void returnRequest(long id) {
        Request request = this.requestDao.get(id);
        request.setStatus(RequestStatusName.RETURNED.getNameDB());
        request.setDateOfReturn(LocalDateTime.now());
        this.requestDao.update(request);
    }

    @Scheduled(cron = "0 13 * * * 1-5")
    public void sendMessageToBookBack() {
        String status = RequestStatusName.PROCESSED.getNameDB();
        List<Request> requestList = this.requestDao.findAllRequests(status);
        for (Request request : requestList) {
            if(request.getDateOfExtradition().plusMinutes(1).isBefore(LocalDateTime.now())){
                emailSendler.sendMessageToBookBack(request.getUser(),"GET BOOK BACK!");
            }
        }
    }
}
