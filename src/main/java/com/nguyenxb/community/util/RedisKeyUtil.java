package com.nguyenxb.community.util;

public class RedisKeyUtil {
    // 分隔字符
    private static final String SPLIT = ":";
    // 点赞的帖子,实体
    private static final String PREFIX_ENTITY_LIKE = "like:entity";
    // 点赞的用户 实体
    private static final String PREFIX_USER_LIKE = "like:user";
    // 关注目标
    private static final String PREFIX_FOLLOWEE = "followee";
    // 粉丝
    private static final String PREFIX_FOLLOWER = "follower";
    // 验证码
    private static final String PREFIX_KAPTCHA = "kaptcha";
    // 登录凭证
    private static final String PREFIX_TICKET = "ticket";
    // 用户信息
    private static final String PREFIX_USER = "user";
    // UV : Unique Visitor 独立访客 每次访问都统计 hyperLogLog
    private static final String PREFIX_UV = "uv";
    // DAU : Daily Active User , 日活跃用户,每日只记录一次访问情况,bitmap
    private static final String PREFIX_DAU = "dau";


    // 某个实体的赞
    // like:entity:entityType:entityId -> set(userId)
    public static String getEntityLikeKey(int entityType,int entityId){
        return PREFIX_ENTITY_LIKE + SPLIT + entityType + SPLIT +entityId;
    }

    // 某个用户的赞
    // like:user:userId  -> int
    public static String getUserLikeKey(int userId){
        return PREFIX_USER_LIKE + SPLIT + userId;
    }

    // 某个用户关注的实体 , 以当前时间来排序
    // followee:userId:entityType -> zset(entityId,now)
    public static String getFolloweeKey(int userId, int entityType){
        return PREFIX_FOLLOWEE + SPLIT + userId + SPLIT + entityType;
    }

    // 某个实体拥有的粉丝
    // follower:entityType:entityId -> zset(userId,now)
    public static String getFollowerKey(int entityType, int entityId){
        return PREFIX_FOLLOWER + SPLIT + entityType + SPLIT +entityId;
    }

    // owner 用户的临时登录凭证
    // 登录验证码
    public static String getKaptchaKey(String owner){
        return PREFIX_KAPTCHA + SPLIT + owner;
    }

    // 登录的凭证
    public static String getTicketKey(String ticket){
        return PREFIX_TICKET + SPLIT + ticket;
    }

    // 用户
    public static String getUserKey(int userId){
        return PREFIX_USER + SPLIT + userId;
    }

    //访问用户 单日 uv
    public static String getUVKey(String date){
        return PREFIX_UV + SPLIT + date;
    }

    //访问用户 区间 uv
    public static String getUVKey(String startDate,String endDate){
        return PREFIX_UV + SPLIT + startDate + SPLIT + endDate;
    }

    //单日活跃用户 dau
    public static String getDAUKey(String date){
        return PREFIX_DAU + SPLIT + date;
    }

    // 区间活跃用户
    public static String getDAUKey(String startDate,String endDate){
        return PREFIX_DAU + SPLIT + startDate + SPLIT + endDate;
    }



}
