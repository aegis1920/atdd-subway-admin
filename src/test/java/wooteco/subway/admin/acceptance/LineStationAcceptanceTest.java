package wooteco.subway.admin.acceptance;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import wooteco.subway.admin.domain.LineStation;
import wooteco.subway.admin.dto.LineResponse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql("/truncate.sql")
public class LineStationAcceptanceTest {
    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    public static RequestSpecification given() {
        return RestAssured.given().log().all();
    }

    /**
     *     Given 지하철역이 여러 개 추가되어있다.
     *     And 지하철 노선이 추가되어있다.
     *
     *     When 지하철 노선에 지하철역을 등록하는 요청을 한다.
     *     Then 지하철역이 노선에 추가 되었다.
     *
     *     When 지하철 노선의 지하철역 목록 조회 요청을 한다.
     *     Then 지하철역 목록을 응답 받는다.
     *     And 새로 추가한 지하철역을 목록에서 찾는다.
     *
     *     When 지하철 노선에 포함된 특정 지하철역을 제외하는 요청을 한다.
     *     Then 지하철역이 노선에서 제거 되었다.
     *
     *     When 지하철 노선의 지하철역 목록 조회 요청을 한다.
     *     Then 지하철역 목록을 응답 받는다.
     *     And 제외한 지하철역이 목록에 존재하지 않는다.
     */
    @DisplayName("지하철 노선에서 지하철역 추가 / 제외")
    @Test
    void manageLineStation() {
        // given
        createStation("잠실역");
        createStation("종합운동장역");
        createStation("선릉역");
        createStation("강남역");
        createLine("1호선");

        List<LineResponse> lines = getLines();
        LineResponse line = getLine(lines.get(0).getId());
        Long lineId = line.getId();

        // when
        createLineStation(lineId, "2", "1", "10", "10");
        createLineStation(lineId, "3", "2", "10", "10");

        // then
        List<LineStation> stations = getLineStations(lineId);
        assertThat(stations.size()).isEqualTo(3);
        assertThat(stations.get(0).getStationId()).isEqualTo(1);

        // given
        Long stationId = stations.get(0).getStationId();

        // when
        deleteLineStation(lineId, stationId);

        // then
        List<LineStation> restStations = getLineStations(lineId);
        assertThat(restStations.size()).isEqualTo(2);
    }

    private void createStation(String name) {
        Map<String, String> params = new HashMap<>();
        params.put("name", name);

        given().
            body(params).
                contentType(MediaType.APPLICATION_JSON_VALUE).
                accept(MediaType.APPLICATION_JSON_VALUE).
            when().
               post("/stations").
            then().
                log().all().
                statusCode(HttpStatus.CREATED.value());
    }

    private void createLine(String name) {
        Map<String, String> params = new HashMap<>();
        params.put("name", name);
        params.put("startTime", LocalTime.of(5, 30).format(DateTimeFormatter.ISO_LOCAL_TIME));
        params.put("endTime", LocalTime.of(23, 30).format(DateTimeFormatter.ISO_LOCAL_TIME));
        params.put("intervalTime", "10");
        params.put("backgroundColor", "bg-red-800");

        given().
            body(params).
                contentType(MediaType.APPLICATION_JSON_VALUE).
                accept(MediaType.APPLICATION_JSON_VALUE).
            when().
                 post("/lines").
            then().
                log().all().
                statusCode(HttpStatus.CREATED.value());
    }

    private LineResponse getLine(Long id) {
        return given().when().
            get("/lines/" + id).
            then().
                log().all().
                extract().as(LineResponse.class);
    }

    private List<LineResponse> getLines() {
        return
            given().
            when().
                get("/lines").
            then().
                log().all().
                extract().
                jsonPath().getList(".", LineResponse.class);
    }

    private List<LineStation> getLineStations(Long id) {
        return given().
            when().
                get("/lines/" + id + "/stations").
            then().
                log().all().
                statusCode(HttpStatus.OK.value()).
                extract().
                jsonPath().getList(".", LineStation.class);

    }

    private void deleteLineStation(Long lineId, Long stationId) {
        given().
        when().
            delete("/lines/" + lineId + "/stations/" + stationId).
        then().
            log().all().
            statusCode(HttpStatus.NO_CONTENT.value());
    }

    private void createLineStation(Long id, String stationId, String preStationId, String distance, String duration) {
        Map<String, String> params = new HashMap<>();
        params.put("stationId", stationId);
        params.put("preStationId", preStationId);
        params.put("distance", distance);
        params.put("duration", duration);

        given().
            body(params).
            contentType(MediaType.APPLICATION_JSON_VALUE).
            accept(MediaType.APPLICATION_JSON_VALUE).
        when().
            post("/lines/" + id + "/stations").
        then().
            log().all().
            statusCode(HttpStatus.CREATED.value());
    }
}
