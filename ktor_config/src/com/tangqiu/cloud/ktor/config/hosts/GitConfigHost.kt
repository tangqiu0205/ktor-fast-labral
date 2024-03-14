package com.tangqiu.cloud.ktor.config.hosts

import com.tangqiu.cloud.ktor.config.ConfigFeature
import com.tangqiu.cloud.ktor.config.ConfigHost
import com.tangqiu.cloud.ktor.config.loader
import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider

object GitConfigHost : ConfigHost<GitConfigHost.GitConfigProfile> {
    override val profile = GitConfigProfile()

    override fun read(): Source = profile.loader.git(
        profile.gitURLAddr, profile.gitFilePath, profile.gitBranch,
        action = {
            setCredentialsProvider(
                UsernamePasswordCredentialsProvider(
                    profile.gitUserName,
                    profile.gitPassWord
                )
            )
        })

    override fun listen(block: (Source) -> Unit) {}

    class GitConfigProfile : ConfigHost.ConfigHostProfile {
        override var type: ConfigFeature.ConfigType = ConfigFeature.ConfigType.CONF
        var gitURLAddr: String = "" //https://xxx.xx.xx/xxxx.git
        var gitFilePath: String = "/src/main/resources/application-develop.properties"
        var gitBranch: String = "xxx"
        var gitUserName: String = "xxx"
        var gitPassWord: String = "xxx"
    }
}