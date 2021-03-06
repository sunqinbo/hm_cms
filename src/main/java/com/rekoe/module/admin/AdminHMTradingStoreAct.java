package com.rekoe.module.admin;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.http.Http;
import org.nutz.integration.shiro.annotation.NutzRequiresPermissions;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.lang.Files;
import org.nutz.lang.Lang;
import org.nutz.lang.Strings;
import org.nutz.lang.util.NutMap;
import org.nutz.mvc.annotation.At;
import org.nutz.mvc.annotation.Ok;
import org.nutz.mvc.annotation.Param;
import org.nutz.mvc.view.ForwardView;
import org.nutz.mvc.view.UTF8JsonView;
import org.nutz.mvc.view.ViewWrapper;

import com.rekoe.common.Message;
import com.rekoe.common.page.Pagination;
import com.rekoe.domain.HMCuisine;
import com.rekoe.domain.HMRestaurantInfo;
import com.rekoe.domain.HMTradingStore;
import com.rekoe.lucene.LuceneSearchResult;
import com.rekoe.service.HMRestaurantInfoService;
import com.rekoe.service.HMTradingStoreService;
import com.rekoe.service.OrderSearchService;

@IocBean
@At("/admin/hm/tradingstore")
public class AdminHMTradingStoreAct {

	@Inject
	private HMTradingStoreService hmTradingStoreService;

	@Inject
	private HMRestaurantInfoService hmRestaurantInfoService;

	@At
	@Ok("fm:template.admin.hm_96789.tradingstore.list")
	@NutzRequiresPermissions(value = "admin.hm:tradingstore:view", name = "查看列表", tag = "商户商圈", enable = true)
	public Pagination<HMTradingStore> list(@Param("id") long id, @Param(value = "pageNumber", df = "1") int page, HttpServletRequest req) {
		req.setAttribute("id", id);
		return hmTradingStoreService.getPagination(page, Cnd.where("restaurantInfoId", "=", id));
	}

	@At
	@Ok("fm:template.admin.hm_96789.tradingstore.add")
	@NutzRequiresPermissions(value = "admin.hm:tradingstore:add", name = "查看列表", tag = "商户商圈", enable = true)
	public HMRestaurantInfo add(@Param("id") long id, HttpServletRequest req) {
		req.setAttribute("id", id);
		HMRestaurantInfo restaurantInfo = hmRestaurantInfoService.fetch(id);
		return hmRestaurantInfoService.fetchLinks(restaurantInfo, "cuisines");
	}

	@At
	@Ok("json")
	@RequiresPermissions(value = "admin.hm:tradingstore:add")
	public Message o_save(@Param("::tradingstore.") HMTradingStore tradingstore, HttpServletRequest req) {
		if (StringUtils.isBlank(tradingstore.getName())) {
			return Message.error("error.empty", req);
		}
		HMTradingStore temp = hmTradingStoreService.fetch(Cnd.where("name", "=", tradingstore.getName()));
		if (!Lang.isEmpty(temp)) {
			return Message.error("error.duplicate", req);
		}
		if (Lang.isEmpty(tradingstore.getCuisines())) {
			return Message.error("error.empty.cuisines", req);
		}
		tradingstore = hmTradingStoreService.insert(tradingstore);
		hmTradingStoreService.insertRelation(tradingstore, "cuisines");
		return Message.success("admin.message.success", req);
	}

	@At
	@Ok("json")
	@NutzRequiresPermissions(value = "admin.hm:tradingstore:delete", name = "删除商圈", tag = "商户商圈", enable = true)
	public Message deleted(@Param("id") long id, HttpServletRequest req) {
		hmTradingStoreService.delete(id);
		return Message.success("ok", req);
	}

	@At
	@Ok("json")
	@NutzRequiresPermissions(value = "admin.hm:tradingstore:build", name = "构建索引", tag = "商户商圈", enable = true)
	public Message build(HttpServletRequest req) throws IOException {
		orderSearchService.rebuild();
		return Message.success("ok", req);
	}

	@Inject
	private OrderSearchService orderSearchService;

	@Inject
	protected Dao dao;

	@At
	@Ok("json")
	@NutzRequiresPermissions(value = "admin.hm:tradingstore:search", name = "搜索商圈", tag = "商户商圈", enable = true)
	public Object search(@Param("q") String keys) throws Exception {
		if (Strings.isBlank(keys))
			return new ForwardView("/yvr/list");
		List<LuceneSearchResult> results = orderSearchService.search(keys, 5);
		List<Info> list = new ArrayList<Info>();
		for (LuceneSearchResult result : results) {
			HMTradingStore topic = dao.fetch(HMTradingStore.class, Cnd.where("id", "=", result.getId()));
			if (topic == null)
				continue;
			topic = dao.fetchLinks(topic, null);
			Info info = new Info(topic.getId() + "", topic.getRestaurantInfo().getName(), topic.getName(), topic.getHotel(), topic.getPhone(), topic.getAddr());
			for (HMCuisine cuisine : topic.getCuisines()) {
				info.addCuisine(cuisine.getName());
			}
			list.add(info);
		}
		return new ViewWrapper(new UTF8JsonView(), new NutMap().setv("info", list));
	}

	public class Info {
		private String id;
		private String label;
		private String value;
		private String hotel;
		private String phone;
		private List<String> cuisines = new ArrayList<String>();
		private String addr;

		public Info(String id, String label, String value, String hotel, String phone, String addr) {
			super();
			this.id = id;
			this.label = label;
			this.value = value;
			this.hotel = hotel;
			this.phone = phone;
			this.addr = addr;
		}

		public Info addCuisine(String cuisine) {
			cuisines.add(cuisine);
			return this;
		}

		public String getHotel() {
			return hotel;
		}

		public void setHotel(String hotel) {
			this.hotel = hotel;
		}

		public String getPhone() {
			return phone;
		}

		public void setPhone(String phone) {
			this.phone = phone;
		}

		public List<String> getCuisines() {
			return cuisines;
		}

		public void setCuisines(List<String> cuisines) {
			this.cuisines = cuisines;
		}

		public String getAddr() {
			return addr;
		}

		public void setAddr(String addr) {
			this.addr = addr;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getLabel() {
			return label;
		}

		public void setLabel(String label) {
			this.label = label;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}

	@At
	@Ok("fm:template.admin.hm_96789.tradingstore.search")
	@RequiresPermissions("admin.hm:tradingstore:search")
	public void m_s() {
	}
	
	public static void main(String[] args) {
		Reader reader = Http.get("http://code.jquery.com/ui/1.10.4/themes/smoothness/jquery-ui.css").getReader();
		Files.write("d:/jquery-ui.css", reader);
	}
}
