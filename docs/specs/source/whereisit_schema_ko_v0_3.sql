-- 어디갔지: 6개 테이블 간소화안 v0.3 한글 식별자 버전
-- 원본 v0.3의 테이블·컬럼·인덱스·제약 이름만 한글화했습니다.
-- 컬럼 순서·타입·NULL·기본값·상태 코드·키 관계·검사식은 유지합니다.
-- PRIMARY KEY 등 SQL 문법 및 SEARCHING/POLICE 등의 저장값은 번역하지 않습니다.
-- 기존 영문 테이블을 자동으로 이름 변경하거나 데이터를 이전하지 않습니다.
-- MySQL 8.0.16 이상 / 신규 빈 스키마용
-- 기존 10개 테이블을 변경하는 마이그레이션이 아닙니다.
-- 애플리케이션은 DATETIME 값을 UTC로 저장합니다.
-- 아래 time_zone 설정은 현재 SQL 실행 연결에만 적용됩니다.

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- 1. 회원
CREATE TABLE `회원` (
    `회원_번호` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '회원 ID',
    `이메일` VARCHAR(254) NOT NULL COMMENT '로그인 이메일',
    `비밀번호_해시` VARCHAR(255) NOT NULL COMMENT '비밀번호 해시',
    `사용_언어_코드` VARCHAR(35) NOT NULL COMMENT '사용자가 선택한 지원 언어',
    `가입_시각` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '가입 시각',
    `수정_시각` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 시각',

    PRIMARY KEY (`회원_번호`),
    UNIQUE KEY `고유_회원_이메일` (`이메일`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='회원';

-- 2. 분실물 검색·추적
-- 검색 시작 시 SEARCHING, 추적 신청 시 TRACKING으로 변경합니다.
CREATE TABLE `분실물_검색_추적` (
    `분실물_번호` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '분실물 검색·추적 ID',
    `회원_번호` BIGINT UNSIGNED NOT NULL COMMENT '소유 회원 ID',
    `분실물_설명` TEXT NOT NULL COMMENT '현재 분실물 설명',
    `사용_언어_코드` VARCHAR(35) NOT NULL COMMENT '검색 결과·알림 언어',
    `물품_대분류_코드` VARCHAR(6) NULL COMMENT '공통 검색조건: 대분류',
    `물품_중분류_코드` VARCHAR(6) NULL COMMENT '공통 검색조건: 중분류',
    `색상_코드` VARCHAR(8) NULL COMMENT '공통 검색조건: 색상',
    `지역_코드` VARCHAR(6) NULL COMMENT '공통 검색조건: 지역',
    `분실_기간_시작일` DATE NULL COMMENT '사용자가 기억하는 분실 기간 시작일',
    `분실_기간_종료일` DATE NULL COMMENT '사용자가 기억하는 분실 기간 종료일',
    `분실_장소_설명` VARCHAR(255) NULL COMMENT '사용자가 설명한 분실 장소',
    `습득물_조회_시작일` DATE NULL COMMENT '자동 조회할 습득일 범위의 시작일',
    `알림_이메일` VARCHAR(254) NULL COMMENT '추적 등록 시 확정할 알림 수신 이메일',
    `검색_추적_상태` VARCHAR(16) COLLATE utf8mb4_bin NOT NULL DEFAULT 'SEARCHING'
        COMMENT 'SEARCHING / TRACKING / EXPIRED',
    `추적_시작_시각` DATETIME(6) NULL COMMENT '자동 추적 등록 시각',
    `추적_만료_시각` DATETIME(6) NULL COMMENT '추적 등록 시각 + 7일',
    `최근_자동검색_완료일` DATE NULL COMMENT '최근 정상 완료한 자동 검색일: 한국 날짜',
    `삭제_시각` DATETIME(6) NULL COMMENT '사용자 삭제 시각: 논리 삭제',
    `생성_시각` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '검색 건 생성 시각',
    `수정_시각` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 시각',

    PRIMARY KEY (`분실물_번호`),
    KEY `인덱스_분실물_회원` (`회원_번호`, `삭제_시각`, `생성_시각`),
    KEY `인덱스_분실물_추적` (`검색_추적_상태`, `삭제_시각`, `추적_만료_시각`, `최근_자동검색_완료일`),
    CONSTRAINT `외래키_분실물_회원`
        FOREIGN KEY (`회원_번호`) REFERENCES `회원` (`회원_번호`),
    CONSTRAINT `검사_분실물_상태`
        CHECK (`검색_추적_상태` IN ('SEARCHING', 'TRACKING', 'EXPIRED')),
    CONSTRAINT `검사_분실물_분실기간`
        CHECK (`분실_기간_시작일` IS NULL OR `분실_기간_종료일` IS NULL
               OR `분실_기간_시작일` <= `분실_기간_종료일`),
    CONSTRAINT `검사_분실물_추적기간`
        CHECK (
            (`검색_추적_상태` = 'SEARCHING' AND `추적_시작_시각` IS NULL AND `추적_만료_시각` IS NULL)
            OR
            (`검색_추적_상태` IN ('TRACKING', 'EXPIRED')
             AND `추적_시작_시각` IS NOT NULL AND `추적_만료_시각` IS NOT NULL
             AND `추적_만료_시각` = DATE_ADD(`추적_시작_시각`, INTERVAL 7 DAY)
             AND `알림_이메일` IS NOT NULL AND `습득물_조회_시작일` IS NOT NULL)
        )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='분실물 검색 및 7일 자동 추적';

-- 3. 대화 메시지
CREATE TABLE `대화_메시지` (
    `메시지_번호` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '메시지 ID',
    `분실물_번호` BIGINT UNSIGNED NOT NULL COMMENT '대화 대상 분실물 ID',
    `작성_주체` VARCHAR(16) COLLATE utf8mb4_bin NOT NULL COMMENT 'USER / ASSISTANT',
    `메시지_내용` TEXT NOT NULL COMMENT '사용자 입력 또는 AI 응답',
    `작성_시각` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '작성 시각',

    PRIMARY KEY (`메시지_번호`),
    KEY `인덱스_대화_분실물` (`분실물_번호`, `메시지_번호`),
    CONSTRAINT `외래키_대화_분실물`
        FOREIGN KEY (`분실물_번호`) REFERENCES `분실물_검색_추적` (`분실물_번호`),
    CONSTRAINT `검사_대화_작성주체`
        CHECK (`작성_주체` IN ('USER', 'ASSISTANT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='분실물별 대화 메시지';

-- 4. 습득물 원본: 두 API의 출처를 반드시 구분합니다.
-- 상세 API 매핑 미확인 항목은 NULL로 두고 추정하지 않습니다.
CREATE TABLE `습득물` (
    `습득물_번호` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '내부 습득물 ID',
    `출처_구분` VARCHAR(16) COLLATE utf8mb4_bin NOT NULL COMMENT 'POLICE / PORTAL',
    `외부_관리_번호` VARCHAR(32) COLLATE utf8mb4_bin NOT NULL COMMENT '외부 관리 ID',
    `외부_습득_순번` VARCHAR(10) COLLATE utf8mb4_bin NOT NULL COMMENT '외부 습득순번: 문자열 보존',
    `물품명` VARCHAR(200) NULL COMMENT '물품명',
    `게시_제목` VARCHAR(300) NULL COMMENT '게시 제목',
    `분류명` VARCHAR(100) NULL COMMENT '분류명 원문',
    `색상명` VARCHAR(100) NULL COMMENT '색상명 원문',
    `습득일` DATE NULL COMMENT '습득일: 분실일과 구분',
    `보관_장소` VARCHAR(255) NULL COMMENT '보관장소 원문',
    `사진_주소` VARCHAR(2048) NULL COMMENT 'API가 제공한 사진 URL',
    `습득_장소` VARCHAR(255) NULL COMMENT '습득 장소: 상세 API 매핑 확인 후 저장',
    `보관기관_연락처` VARCHAR(40) NULL COMMENT '보관기관 연락처: 상세 매핑 확인 후 저장',
    `상세_설명` TEXT NULL COMMENT '상세설명: 상세 API 매핑 확인 후 저장',
    `상세_조회_시각` DATETIME(6) NULL COMMENT '상세 조회 성공 시각',
    `최초_수집_시각` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        COMMENT '우리 서비스의 최초 수집 시각',
    `최근_수집_시각` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        COMMENT '우리 서비스의 최근 수집 시각',

    PRIMARY KEY (`습득물_번호`),
    UNIQUE KEY `고유_습득물_출처식별` (`출처_구분`, `외부_관리_번호`, `외부_습득_순번`),
    CONSTRAINT `검사_습득물_출처`
        CHECK (`출처_구분` IN ('POLICE', 'PORTAL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='일반·포털기관 습득물 원본';

-- 5. 이메일 알림: 후보 테이블이 참조하므로 먼저 생성합니다.
CREATE TABLE `이메일_알림` (
    `알림_번호` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '메일 묶음 ID',
    `분실물_번호` BIGINT UNSIGNED NOT NULL COMMENT '알림 대상 분실물 ID',
    `알림_기준일` DATE NOT NULL COMMENT '일일 메일 묶음 기준일: 한국 날짜',
    `수신_이메일` VARCHAR(254) NOT NULL COMMENT '확정된 수신 이메일',
    `사용_언어_코드` VARCHAR(35) NOT NULL COMMENT '메일 언어',
    `메일_제목` VARCHAR(255) NOT NULL COMMENT '확정된 메일 제목',
    `메일_본문` TEXT NOT NULL COMMENT '후보 n건 안내 본문',
    `발송_상태` VARCHAR(16) COLLATE utf8mb4_bin NOT NULL DEFAULT 'PENDING'
        COMMENT 'PENDING / SENDING / SENT / FAILED / UNKNOWN / CANCELLED',
    `발송_시도_횟수` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '발송 시도 횟수',
    `오류_코드` VARCHAR(64) NULL COMMENT '민감정보를 제외한 발송 오류 코드',
    `발송_접수_시각` DATETIME(6) NULL COMMENT '메일 제공자 접수 성공 시각: 수신·열람 보장 아님',
    `생성_시각` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 시각',
    `수정_시각` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 시각',

    PRIMARY KEY (`알림_번호`),
    UNIQUE KEY `고유_이메일_분실물_기준일` (`분실물_번호`, `알림_기준일`),
    KEY `인덱스_이메일_상태` (`발송_상태`, `생성_시각`),
    CONSTRAINT `외래키_이메일_분실물`
        FOREIGN KEY (`분실물_번호`) REFERENCES `분실물_검색_추적` (`분실물_번호`),
    CONSTRAINT `검사_이메일_상태`
        CHECK (`발송_상태` IN ('PENDING', 'SENDING', 'SENT', 'FAILED', 'UNKNOWN', 'CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='신규 유사 후보 이메일 알림';

-- 6. 분실물별 후보: 최근 추천 결과와 중복 알림 방지에 필요한 정보만 관리합니다.
CREATE TABLE `분실물_후보` (
    `후보_번호` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '분실물별 후보 ID',
    `분실물_번호` BIGINT UNSIGNED NOT NULL COMMENT '사용자 분실물 ID',
    `습득물_번호` BIGINT UNSIGNED NOT NULL COMMENT '실제 조회한 습득물 ID',
    `추천_순위` INT UNSIGNED NULL COMMENT '최근 검색의 추천 순위',
    `추천_이유` TEXT NULL COMMENT '사용자 언어로 작성한 추천 이유',
    `유사_후보_여부` TINYINT UNSIGNED NULL COMMENT '1: 유사 후보, 0: 비유사, NULL: 미평가',
    `현재_결과_여부` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '최근 성공한 검색 결과에 포함되면 1',
    `추적전_확인_후보_여부` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '추적 등록 전에 이미 보여준 후보면 1',
    `알림_번호` BIGINT UNSIGNED NULL COMMENT '이 후보를 포함한 메일 묶음 ID',
    `최초_확인_시각` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '이 분실물에서 최초 확인',
    `최근_확인_시각` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '이 분실물에서 최근 확인',

    PRIMARY KEY (`후보_번호`),
    UNIQUE KEY `고유_후보_분실물_습득물` (`분실물_번호`, `습득물_번호`),
    KEY `인덱스_후보_현재결과` (`분실물_번호`, `현재_결과_여부`, `추천_순위`),
    KEY `인덱스_후보_습득물` (`습득물_번호`),
    KEY `인덱스_후보_알림` (`알림_번호`),
    CONSTRAINT `외래키_후보_분실물`
        FOREIGN KEY (`분실물_번호`) REFERENCES `분실물_검색_추적` (`분실물_번호`),
    CONSTRAINT `외래키_후보_습득물`
        FOREIGN KEY (`습득물_번호`) REFERENCES `습득물` (`습득물_번호`),
    CONSTRAINT `외래키_후보_알림`
        FOREIGN KEY (`알림_번호`) REFERENCES `이메일_알림` (`알림_번호`),
    CONSTRAINT `검사_후보_순위`
        CHECK (`추천_순위` IS NULL OR `추천_순위` >= 1),
    CONSTRAINT `검사_후보_여부값`
        CHECK ((`유사_후보_여부` IS NULL OR `유사_후보_여부` IN (0, 1))
               AND `현재_결과_여부` IN (0, 1) AND `추적전_확인_후보_여부` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='분실물별 추천 후보와 알림 연결';
