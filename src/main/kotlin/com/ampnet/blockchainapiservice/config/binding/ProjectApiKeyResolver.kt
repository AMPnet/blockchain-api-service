package com.ampnet.blockchainapiservice.config.binding

import com.ampnet.blockchainapiservice.config.binding.annotation.ApiKeyBinding
import com.ampnet.blockchainapiservice.exception.NonExistentApiKeyException
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.repository.ApiKeyRepository
import com.ampnet.blockchainapiservice.repository.ProjectRepository
import org.springframework.core.MethodParameter
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import javax.servlet.http.HttpServletRequest

class ProjectApiKeyResolver(
    private val apiKeyRepository: ApiKeyRepository,
    private val projectRepository: ProjectRepository
) : HandlerMethodArgumentResolver {

    companion object {
        const val API_KEY_HEADER = "X-API-KEY"
    }

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.parameterType == Project::class.java &&
            parameter.hasParameterAnnotation(ApiKeyBinding::class.java)
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        nativeWebRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): Project {
        val httpServletRequest = nativeWebRequest.getNativeRequest(HttpServletRequest::class.java)
        // TODO check if API has expired/used up/etc. - out of scope for MVP
        val apiKey = httpServletRequest?.getHeader(API_KEY_HEADER)?.let { apiKeyRepository.getByValue(it) }
            ?: throw NonExistentApiKeyException()
        return projectRepository.getById(apiKey.projectId)!! // non-null enforced by foreign key constraint in DB
    }
}
