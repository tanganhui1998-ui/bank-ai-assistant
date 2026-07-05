create table if not exists ai_retrieval_feedback (
  feedback_id varchar(64) primary key comment '反馈ID',
  user_id varchar(64) not null comment '用户ID',
  user_name varchar(128) null comment '用户姓名',
  conversation_id varchar(64) null comment '会话ID',
  question varchar(1000) not null comment '用户问题',
  helpful tinyint(1) null comment '是否有帮助',
  rating int null comment '评分，1-5',
  comment varchar(1000) null comment '用户补充意见',
  chunk_ids varchar(2000) null comment '本次反馈关联的切片ID列表',
  created_time datetime not null comment '创建时间',
  index idx_feedback_user_time (user_id, created_time),
  index idx_feedback_question (question(128)),
  index idx_feedback_rating (rating, helpful)
) engine=InnoDB default charset=utf8mb4 comment='RAG检索反馈表';
