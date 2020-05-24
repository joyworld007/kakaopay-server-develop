package com.kakaopay.server.service.coupon;

import com.kakaopay.server.domain.common.Result;
import com.kakaopay.server.domain.coupon.CouponStatus;
import com.kakaopay.server.domain.coupon.ResultCode;
import com.kakaopay.server.domain.coupon.converter.CouponConverter;
import com.kakaopay.server.domain.coupon.dao.CouponDao;
import com.kakaopay.server.domain.coupon.dto.CouponDto;
import com.kakaopay.server.domain.coupon.entity.Coupon;
import com.kakaopay.server.domain.coupon.entity.CouponIssue;
import com.kakaopay.server.repository.coupon.CouponJdbcRepository;
import com.kakaopay.server.repository.coupon.CouponJpaRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CouponServiceImpl implements CouponService {

  @Autowired
  CouponJdbcRepository couponJdbcRepository;

  @Autowired
  CouponJpaRepository couponJpaRepository;

  final static int BATCH_SIZE = 10000;

  @Override
  @Transactional
  public ResultCode creat(Long size) {

    LocalDateTime expireDate = LocalDateTime.now();
    List<CouponDao> couponDaoList = new ArrayList<>();
    try {
      for (long i = 1; i <= size; i++) {
        couponDaoList.add(
            CouponDao.builder()
                .expireDate(expireDate)
                .status(CouponStatus.CREATED).build()
        );
        if (i % BATCH_SIZE == 0) {
          couponJdbcRepository.createCoupon(couponDaoList);
          expireDate = LocalDateTime.now().plusDays(1);
          couponDaoList.clear();
        }
      }
      couponJdbcRepository.createCoupon(couponDaoList);
    } catch (Exception e) {
      return ResultCode.FAIL;
    }
    return ResultCode.SUCCESS;
  }

  @Override
  @Transactional
  public ResultCode updateCoupon(Long id, CouponDto couponDto) {
    Optional<Coupon> coupon = couponJpaRepository.findById(id);
    if (coupon.isPresent()) {
      //쿠폰 사용기간 만료 체크
      if (coupon.get().isExpired()) {
        return ResultCode.COUPON_EXPIRED;
      }
      if (CouponStatus.ISSUED.equals(couponDto.getStatus())
          && CouponStatus.CREATED.equals(coupon.get().getStatus())
          && Optional.ofNullable(couponDto.getUserId()).isPresent()) {
        //사용자 에게 쿠폰 지급
        //쿠폰 발급일 업데이트
        couponDto.setIssueDate(LocalDateTime.now());
        //쿠폰을 발급 처리
        coupon.get().setCouponIssue(CouponIssue.ofDto(couponDto));
      } else if (CouponStatus.USED.equals(couponDto.getStatus())
          && CouponStatus.ISSUED.equals(coupon.get().getStatus())) {
        //쿠폰 사용 처리
        couponDto.setIssueDate(coupon.get().getCouponIssue().getIssueDate());
        couponDto.setUseDate(LocalDateTime.now());
        couponDto.setUserId(coupon.get().getCouponIssue().getUserId());
        coupon.get().setCouponUse(CouponIssue.ofDto(couponDto));
      } else if (CouponStatus.ISSUED.equals(couponDto.getStatus())
          && CouponStatus.USED.equals(coupon.get().getStatus())) {
        //쿠폰 사용 취소
        couponDto.setIssueDate(coupon.get().getCouponIssue().getIssueDate());
        couponDto.setUserId(coupon.get().getCouponIssue().getUserId());
        couponDto.setUseDate(null);
        //쿠폰을 사용 취소 처리
        coupon.get().setCouponCancel(CouponIssue.ofDto(couponDto));
      } else {
        return ResultCode.BAD_REQUEST;
      }
      couponJpaRepository.save(coupon.get());
    } else {
      return ResultCode.COUPON_NOT_FOUND;
    }
    return ResultCode.SUCCESS;
  }

  @Override
  public Result findCouponByUserId(String UserId) {
    return Result.builder().entry(
        new CouponConverter()
            .covertFromEntities(couponJpaRepository.findAllByCouponIssue_UserId(UserId))
    ).build();
  }

  @Override
  public ResultCode cancelCoupon(Long CouponId) {
    return ResultCode.SUCCESS;
  }

  @Override
  public List<Coupon> selectTodayExpiredCoupon() {
    return null;
  }

}
