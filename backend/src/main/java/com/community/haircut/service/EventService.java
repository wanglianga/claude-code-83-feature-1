package com.community.haircut.service;

import com.community.haircut.common.BizException;
import com.community.haircut.entity.CommunityEvent;
import com.community.haircut.entity.ServiceOrder;
import com.community.haircut.enums.OrderType;
import com.community.haircut.repository.CommunityEventRepository;
import com.community.haircut.repository.ServiceOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EventService {

    private final CommunityEventRepository eventRepository;
    private final ServiceOrderRepository orderRepository;
    private final OrderService orderService;

    public EventService(CommunityEventRepository eventRepository, ServiceOrderRepository orderRepository,
                        OrderService orderService) {
        this.eventRepository = eventRepository;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    public List<Map<String, Object>> list() {
        return eventRepository.findAllByOrderByEventDateDesc().stream().map(e -> {
            Map<String, Object> map = new HashMap<>();
            map.put("event", e);
            map.put("bookedCount", orderRepository.findByEventId(e.getId()).size());
            return map;
        }).toList();
    }

    @Transactional
    public CommunityEvent create(CommunityEvent event) {
        event.setId(null);
        event.setStatus("OPEN");
        return eventRepository.save(event);
    }

    @Transactional
    public CommunityEvent updateStatus(Long id, String status) {
        CommunityEvent event = eventRepository.findById(id).orElseThrow(() -> new BizException("活动不存在"));
        event.setStatus(status);
        return eventRepository.save(event);
    }

    /** 活动报名：为老人生成 EVENT 类型服务单 */
    @Transactional
    public ServiceOrder book(Long eventId, Long elderId, OrderService.LoginUserInfo operator) {
        CommunityEvent event = eventRepository.findById(eventId).orElseThrow(() -> new BizException("活动不存在"));
        if (!"OPEN".equals(event.getStatus())) {
            throw new BizException("活动已停止报名");
        }
        List<ServiceOrder> booked = orderRepository.findByEventId(eventId);
        if (booked.size() >= event.getMaxElders()) {
            throw new BizException("活动名额已满");
        }
        boolean already = booked.stream().anyMatch(o -> o.getElderId().equals(elderId));
        if (already) {
            throw new BizException("该老人已报名本次活动");
        }
        return orderService.createOrder(elderId, event.getEventDate(), event.getTimeSlot(),
                OrderType.EVENT, eventId, null, operator);
    }
}
