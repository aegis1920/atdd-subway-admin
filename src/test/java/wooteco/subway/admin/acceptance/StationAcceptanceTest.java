package wooteco.subway.admin.acceptance;

import static org.assertj.core.api.Assertions.*;

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
import wooteco.subway.admin.dto.StationResponse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql("/truncate.sql")
public class StationAcceptanceTest {
    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    public static RequestSpecification given() {
        return RestAssured.given().log().all();
    }

    @DisplayName("지하철역을 관리한다")
    @Test
    void manageStation() {
        // when
        createStation("잠실역");
        createStation("종합운동장역");
        createStation("선릉역");
        createStation("강남역");

        // then
        List<StationResponse> stations = getStations();
        assertThat(stations.size()).isEqualTo(4);

        // when
        deleteStation(stations.get(0).getId());

        // then
        List<StationResponse> stationsAfterDelete = getStations();
        assertThat(stationsAfterDelete.size()).isEqualTo(3);
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

    private List<StationResponse> getStations() {
        return given().
                when().
                    get("/stations").
                then().
                    log().all().
                    extract().
                    jsonPath().getList(".", StationResponse.class);
    }

    private void deleteStation(Long id) {
        given().
        when().
                delete("/stations/" + id).
        then().
                log().all().
               statusCode(HttpStatus.NO_CONTENT.value());

    }

    @DisplayName("지하철 역 이름 생성_DuplicateName_Should 서버에서 400 에러를 보냄")
    @Test
    void duplicateStationName() {
        createStation("종각역");

        Map<String, String> params = new HashMap<>();
        params.put("name", "종각역");

        given().
            body(params).
            contentType(MediaType.APPLICATION_JSON_VALUE).
            accept(MediaType.APPLICATION_JSON_VALUE).
            when().
            post("/stations").
            then().
            log().all().
            statusCode(HttpStatus.BAD_REQUEST.value());
    }
}
