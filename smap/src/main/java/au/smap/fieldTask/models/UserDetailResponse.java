package au.smap.fieldTask.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class UserDetailResponse {

	public String message;
	public String status;
	@SerializedName("data")
	public List<UserDetail> userList;
}
