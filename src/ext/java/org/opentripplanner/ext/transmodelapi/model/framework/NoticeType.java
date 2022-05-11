package org.opentripplanner.ext.transmodelapi.model.framework;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import org.opentripplanner.ext.transmodelapi.support.GqlUtil;
import org.opentripplanner.model.Notice;

public class NoticeType {

  public static GraphQLObjectType create() {
    return GraphQLObjectType
      .newObject()
      .name("Notice")
      .field(GqlUtil.newTransitIdField())
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("text")
          .type(Scalars.GraphQLString)
          .dataFetcher(environment -> ((Notice) environment.getSource()).getText())
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("publicCode")
          .type(Scalars.GraphQLString)
          .dataFetcher(environment -> ((Notice) environment.getSource()).getPublicCode())
          .build()
      )
      .build();
  }
}
