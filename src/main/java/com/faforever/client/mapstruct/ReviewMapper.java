package com.faforever.client.mapstruct;

import com.faforever.client.domain.MapReviewsSummaryBean;
import com.faforever.client.domain.MapVersionReviewBean;
import com.faforever.client.domain.MapVersionReviewsSummaryBean;
import com.faforever.client.domain.ModReviewsSummaryBean;
import com.faforever.client.domain.ModVersionReviewBean;
import com.faforever.client.domain.ModVersionReviewsSummaryBean;
import com.faforever.client.domain.ReplayReviewBean;
import com.faforever.client.domain.ReplayReviewsSummaryBean;
import com.faforever.commons.api.dto.GameReview;
import com.faforever.commons.api.dto.GameReviewsSummary;
import com.faforever.commons.api.dto.MapReviewsSummary;
import com.faforever.commons.api.dto.MapVersionReview;
import com.faforever.commons.api.dto.MapVersionReviewsSummary;
import com.faforever.commons.api.dto.ModReviewsSummary;
import com.faforever.commons.api.dto.ModVersionReview;
import com.faforever.commons.api.dto.ModVersionReviewsSummary;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = {ReplayMapper.class, ModMapper.class, MapMapper.class, PlayerMapper.class}, config = MapperConfiguration.class)
public interface ReviewMapper {

  @Mapping(target = "replay", source = "game")
  ReplayReviewBean map(GameReview dto, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "game", source = "replay")
  GameReview map(ReplayReviewBean bean, @Context CycleAvoidingMappingContext context);

  MapVersionReviewBean map(MapVersionReview dto, @Context CycleAvoidingMappingContext context);

  MapVersionReview map(MapVersionReviewBean bean, @Context CycleAvoidingMappingContext context);

  ModVersionReviewBean map(ModVersionReview dto, @Context CycleAvoidingMappingContext context);

  ModVersionReview map(ModVersionReviewBean bean, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "numReviews", source = "reviews")
  @Mapping(target = "replay", source = "game")
  ReplayReviewsSummaryBean map(GameReviewsSummary dto, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "reviews", source = "numReviews")
  @Mapping(target = "game", source = "replay")
  GameReviewsSummary map(ReplayReviewsSummaryBean bean, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "numReviews", source = "reviews")
  MapVersionReviewsSummaryBean map(MapVersionReviewsSummary dto, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "reviews", source = "numReviews")
  MapVersionReviewsSummary map(MapVersionReviewsSummaryBean bean, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "numReviews", source = "reviews")
  ModVersionReviewsSummaryBean map(ModVersionReviewsSummary dto, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "reviews", source = "numReviews")
  ModVersionReviewsSummary map(ModVersionReviewsSummaryBean bean, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "numReviews", source = "reviews")
  MapReviewsSummaryBean map(MapReviewsSummary dto, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "reviews", source = "numReviews")
  MapReviewsSummary map(MapReviewsSummaryBean bean, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "numReviews", source = "reviews")
  ModReviewsSummaryBean map(ModReviewsSummary dto, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "reviews", source = "numReviews")
  ModReviewsSummary map(ModReviewsSummaryBean bean, @Context CycleAvoidingMappingContext context);
}
